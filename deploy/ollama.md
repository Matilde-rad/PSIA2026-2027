# deploy/ollama

Ollama is the inference engine behind the `ai` service, and `ai` is its only
client. It serves one small embedding model; there is no LLM. This note gives
the settings for the Ollama pod and what `movies` needs for the vector search.
No manifests are written here.

## What to run

- Image `ollama/ollama`, pinned to a tested version. Local development ran
  0.40.0.
- Model `qwen3-embedding:0.6b` (639 MB). It must be the same model that
  produced `data/embeddings/movielens_qwen3_embedding_0.6b.csv`: query and movie
  vectors from different models do not match.
- Exactly one replica, behind a ClusterIP Service on port 11434.
- A volume at `/root/.ollama`, about 2 GB. Pull the model into it once (an init
  step) instead of on every start.
- Readiness: `GET /api/tags` answers 200 and lists the model.

## Load per search

One live search sends `ai` one embedding request: the user's text plus a fixed
instruction, a few dozen tokens. On an Apple M4 with Metal it takes tens of
milliseconds once the model is loaded. CPU timings on the cluster's nodes are
not measured yet; measure them before sizing.

## Settings

| Setting | Where | Value | Why |
|---------|-------|-------|-----|
| `OLLAMA_KEEP_ALIVE` | Ollama pod | `30m` | The default is 5 minutes. `ai` already sends `keep_alive` 30m per request (`ai.options.keep-alive`), so this is a safety net. |
| `OLLAMA_HOST` | Ollama pod | `0.0.0.0:11434` | Ollama binds 127.0.0.1 by default and must listen on all interfaces inside the pod. Expose it only through the ClusterIP Service. |

Timeouts: `ai` to Ollama is 30 s (`quarkus.rest-client.ollama.read-timeout`),
which leaves room for a cold model load, and `recommendations` to `ai` is 35 s.

## The vector search in `movies`

- `movies_db` needs the pgvector extension: run Postgres from
  `pgvector/pgvector:pg18` (what dev uses) or another image with pgvector.
- `movies` reads the vectors from the CSV at `movies.embeddings.file`
  (`MOVIES_EMBEDDINGS_FILE`), 1.2 GB, kept in Git LFS. The CI checkout must run
  `git lfs pull` before building the image or volume that carries it.
- The first start loads 87,585 vectors and builds the HNSW index, about 1.5 min
  on an M4. Later starts skip it when every movie already has a vector, so give
  `movies_db` a persistent volume.
- The index build sets `maintenance_work_mem` to 1 GB for its transaction, and
  the vectors alone take about 360 MB, so the Postgres pod needs that memory
  headroom. It builds without parallel workers, because they need more than the
  default 64 MB of `/dev/shm`.

## Config the other services need

| Service | Set in the cluster | Why |
|---------|--------------------|-----|
| `ai` | `QUARKUS_REST_CLIENT_OLLAMA_URL=http://<ollama-service>:11434` | There is no `%prod` override, and the default is `http://127.0.0.1:11434`. |
| `ai` (optional) | `AI_EMBED_MODEL`, `AI_OPTIONS_KEEP_ALIVE`, `QUARKUS_REST_CLIENT_OLLAMA_READ_TIMEOUT` | Tuning knobs, all with working defaults. |
| `movies` | `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD`, `MOVIES_EMBEDDINGS_FILE`, egress to `files.grouplens.org` on first start, and a writable temp dir | There is no `%prod` datasource. It seeds at startup when its table is empty. |
| `recommendations` | The same three datasource variables for `recs_db`, `RECS_AGENT_EVERY` (default `10m`), and exactly one replica | The agent's no-overlap guard is per instance. Its `%prod` client URLs are already `http://ai:8080`, `http://movies:8080` and `http://users:8080`. |
| probes | `users`, `movies` and `recommendations`: `/q/health/live`, `/q/health/ready` and `/q/health/started`. `ai`: a TCP probe on 8080 | `ai` has no health extension. |

Supply credentials through Secrets, and never reuse the Dev Services throwaway
login.

## Assumptions to confirm

- CPU latency of the embedding model on the real nodes.
- Whether GitLab on gitlab.rnl serves Git LFS objects to CI.
- Ollama has no built-in authentication, and its API can pull and delete models.
  So never expose it through an Ingress, NodePort or LoadBalancer: ClusterIP
  reachable from `ai` only.
- Whether the image already sets the host variable.
