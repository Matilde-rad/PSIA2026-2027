# deploy/ollama

Ollama is the inference engine behind the `ai` service, and `ai` is its only
client. This note gives sizing, scheduling and settings for the Ollama pod,
plus the CPU timings measured so far. No manifests are written here.

## What to run

- Image `ollama/ollama:0.35.1`, the version the timings below were measured on.
  Local development also ran 0.40.0.
- Model `qwen3:4b-instruct-2507-q4_K_M` (Q4_K_M, 2,497,293,803 bytes on disk).
  Always use the full tag: the bare `qwen3:4b` tag is the thinking build.
- Exactly one replica, behind a ClusterIP Service on port 11434.
- A volume at `/root/.ollama`, about 5 GB. Pull the model into it once (an init
  step) instead of on every start.
- Readiness: `GET /api/tags` answers 200 and lists the model. The first call
  after a model load takes 13 to 21 s longer.

## Sizing

- CPU: a 4 vCPU limit is the measured baseline. During generation Ollama used
  396.53 % of the 400 % quota, so it uses all of it. More cores were not
  measured.
- RAM: 4.2 GB resident at context 8192 (`ollama ps`), and 4.056 GiB under load
  inside a 6 GiB limit (67.59 %). Recommend a 6 GiB memory limit on a node with
  8 GB or more. RAM grows with `OLLAMA_NUM_PARALLEL` times
  `OLLAMA_CONTEXT_LENGTH` (Ollama FAQ).
- Example machine: `n2d-standard-4` (4 vCPU, 16 GB). This is the infra owner's
  call.

## Own node

Run Ollama on its own node, for these reasons:

- Generation saturates the CPU quota (396.53 %), so a co-located pod gets
  starved.
- About 4 GB of memory stays resident.
- A cold load takes 13 to 21 s, so evictions and reschedules are costly.
- With parallelism 1, requests queue FIFO, and the background agent and live
  searches share that single slot.
- The cluster has no GPU path.

Recommend a taint on the node plus a matching toleration and nodeSelector on
the Ollama pod.

## Keep-alive and parallelism

| Setting | Where | Value | Why |
|---------|-------|-------|-----|
| `OLLAMA_NUM_PARALLEL` | Ollama pod | `1` (default) | Matches the measurements; each extra slot adds KV-cache memory. |
| `OLLAMA_MAX_QUEUE` | Ollama pod | `512` (default) | A queued request waits behind the running one, and that wait counts against the caller's read timeout. |
| `OLLAMA_CONTEXT_LENGTH` | Ollama pod | `8192` | Matches `ai.options.num-ctx=8192`, which `ai` sends per request. |
| `OLLAMA_KEEP_ALIVE` | Ollama pod | `30m` | The default is 5 minutes. `ai` already sends `keep_alive` 30m per request (`ai.options.keep-alive`), so this is a safety net. `-1` (never unload) is an option on a dedicated node. |
| `OLLAMA_HOST` | Ollama pod | `0.0.0.0:11434` | Ollama binds 127.0.0.1 by default and must listen on all interfaces inside the pod. Expose it only through the ClusterIP Service. |
| `AI_OPTIONS_NUM_THREAD` | `ai` pod | the Ollama CPU limit (e.g. `4`) | Ollama sizes its threads from the host's cores, not from the CPU quota. With 4 threads under a 4-CPU quota, the distinct-prompt rank median went from 25.20 s to 11.39 s. Confirm this on the real node. |

Timeouts: the `ai` to Ollama read timeout is 60 s
(`quarkus.rest-client.ollama.read-timeout=60000`) and `recommendations` to `ai`
is 65 s. A live search makes two `ai` calls, so its worst case is about 130 s,
and `edge`'s read timeout must exceed that if `edge` ever proxies search.

## Measured CPU timings

- Measured on 2026-10-04 on a MacBook Pro M4 (10 cores, 16 GB) in Docker
  Desktop (VM with 10 CPUs and about 7.65 GiB).
- Container `ollama/ollama:0.35.1` with `--cpus=4 -m 6g`,
  `OLLAMA_NUM_PARALLEL=1`, `OLLAMA_CONTEXT_LENGTH=8192`,
  `OLLAMA_KEEP_ALIVE=30m`, and the model above.
- Figures are medians of curl wall time to `ai`, with sample sizes from 2 to 15.

| Quantity | default threads (10) | num_thread=4 |
|----------|----------------------|--------------|
| Cold model load (`load_ms`) | 13.15 s (run 1 wall 26.07 s) | 20.52 s (reload after option change, run 17 wall 27.45 s) |
| Median warm /extract-filters | 2.19 s (n=15, range 1.67 to 2.98 s) | 1.26 s (n=2: 1.17, 1.35 s) |
| Median /rank, 15 candidates, identical repeat (prompt cached) | 15.37 s (n=5, range 13.73 to 16.78 s) | 6.57 s (n=2: 6.70, 6.44 s) |
| Median /rank, 15 candidates, distinct prompt (live-search shape) | 25.20 s (n=3: 25.20, 24.85, 34.87 s) | 11.39 s (n=3: 11.39, 12.73, 9.42 s) |
| /rank, 15 candidates, first prompt (fixture A) | 25.58 s (n=1) | 13.39 s (n=1) |
| /rank, 30 candidates | 36.17 s first, 18.14 s and 31.29 s repeats (median of 3: 31.29 s) | 13.35 s first, 8.23 s repeat (n=2) |
| Generation speed in /rank | 4.4 to 9.1 tok/s | 17.2 to 23.5 tok/s |
| Implied sequential search (median warm extract plus median warm rank with 15) | 17.56 s with the cached rank prompt, 27.39 s with a distinct rank prompt | 7.83 s with the cached rank prompt, 12.65 s with a distinct rank prompt |

- These numbers are indicative only: they come from Apple-silicon vCPUs inside
  Docker Desktop's Linux VM, not GCP x86. `--cpus=4` is the same quota
  mechanism as `limits.cpu: 4`, but the instruction set and clock differ, so
  re-measure on the real node.
- The rank rows are an upper bound for today's code: those rank calls generated
  a one-sentence reason per pick (104 to 158 tokens per 3-pick answer). The
  reason has since been removed. `/rank` now answers `{picks:[{movieId}]}` with
  `ai.rank.num-predict=200`, so a rank call today generates fewer tokens.
- The distinct-prompt rows are the realistic live-search figure.

## Config the other services need

| Service | Set in the cluster | Why |
|---------|--------------------|-----|
| `ai` | `QUARKUS_REST_CLIENT_OLLAMA_URL=http://<ollama-service>:11434` | There is no `%prod` override, and the default is `http://127.0.0.1:11434`. |
| `ai` (optional) | `AI_OPTIONS_NUM_THREAD`, `AI_OPTIONS_KEEP_ALIVE`, `AI_MODEL`, `QUARKUS_REST_CLIENT_OLLAMA_READ_TIMEOUT` | Tuning knobs, all with working defaults. |
| `movies` | `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD`, egress to `files.grouplens.org` on first start, and a writable temp dir | There is no `%prod` datasource. It seeds at startup when its table is empty. |
| `recommendations` | The same three datasource variables for `recs_db`, `RECS_AGENT_EVERY` (default `10m`), and exactly one replica | The agent's no-overlap guard is per instance. Its `%prod` client URLs are already `http://ai:8080`, `http://movies:8080` and `http://users:8080`. |
| probes | `users`, `movies` and `recommendations`: `/q/health/live`, `/q/health/ready` and `/q/health/started`. `ai`: a TCP probe on 8080 | `ai` has no health extension. |

Supply credentials through Secrets, and never reuse the Dev Services throwaway
login.

## Assumptions to confirm

- x86 vCPUs may behave differently from the Apple-silicon numbers.
- The machine type example `n2d-standard-4`.
- The 6 GiB limit and the node with 8 GB or more.
- The 5 GB volume.
- `OLLAMA_KEEP_ALIVE=-1` on a dedicated node.
- Ollama has no built-in authentication, and its API can pull and delete models.
  So never expose it through an Ingress, NodePort or LoadBalancer: ClusterIP
  reachable from `ai` only.
- Whether the image already sets the host variable.
- The taint and nodeSelector mechanics.
