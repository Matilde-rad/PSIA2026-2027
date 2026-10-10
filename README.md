# g2 — AI-powered Movie Recommendation Hub

PSIA 2026/27 project, group 2.

> **Status:** the AI and recommendations slice (`ai`, `movies`,
> `recommendations` and a `users` stub) runs locally; see *AI and
> recommendations slice: run locally*. Registration, login, the web UI and the
> deployment are not built yet. See *Current state* below.

Users register, create a profile describing their interests, and ask the
platform's AI for movie recommendations matching that profile. Users can browse
other users' profiles. A background agent proactively generates and caches
recommendations so they are ready at login.

## Structure

```
app/                Maven reactor (Java 21, Quarkus 3.39.4)
  services/users/     accounts, login, profiles          → users_db
  services/movies/    movie catalog                      → movies_db
  services/recommendations/  AI recommendations + cache  → recs_db
  services/ai/        wraps the Ollama model (no database)
  edge/               aggregates services for the UI (no database)
frontend/web/       web UI — framework not chosen yet         (empty)
infra/terraform/    VMs for the cluster                       (empty)
infra/ansible/      installs software/configs onto those VMs  (empty)
deploy/             Kubernetes manifests, monitoring          (Ollama sizing note only)
```

Each microservice owns a dedicated database instance. `edge` is
the backend-for-frontend: an aggregator rather than a domain service, so it has
no database.

groupId `pt.ulisboa.tecnico.psia.g2` · version via `${revision}`

## Build

```sh
cd app
mvn clean install                     # whole reactor
mvn -pl services/users clean install  # one module on its own
```

The five modules are independent no shared module, so no `-am` needed.

## Run locally (dev mode)

Dev ports are distinct so everything can run at once:

```sh
cd app/services/users           && mvn quarkus:dev   # :8081
cd app/services/ai              && mvn quarkus:dev   # :8084
cd app/services/movies          && mvn quarkus:dev   # :8082
cd app/services/recommendations && mvn quarkus:dev   # :8083
cd app/edge                     && mvn quarkus:dev   # :8080
```

For the AI slice's prerequisites, start order and checks, see *AI and
recommendations slice: run locally* below.

Only `%dev.quarkus.http.port` differs the packaged/container port is 8080
everywhere.

> A Docker container on this machine may already publish `*:8080`, in which case
> `http://localhost:8080` resolves to it over IPv6 while Quarkus binds
> `127.0.0.1:8080`. Use `http://127.0.0.1:8080`.

```sh
curl http://127.0.0.1:8081/users/42
curl http://127.0.0.1:8082/movies/genres
curl http://127.0.0.1:8083/recommendations/users/1
curl http://127.0.0.1:8080/screens/profile/42
```

`edge` calls `users` and `recommendations` concurrently via
`Uni.combine().all().unis(...)`.

`users`, `movies` and `recommendations` expose `/q/health`, `/q/openapi` and
`/q/swagger-ui` (the last in dev only).
`ai` and `edge` carry neither the health nor the OpenAPI extension, so they have
none of them.

## AI and recommendations slice: run locally

This starts the `users` stub, `ai`, `movies` and `recommendations` in dev mode
against a local Ollama. `edge` is not needed and is not started here. Tested on
macOS (Apple M4, 16 GB).

### Prerequisites

- JDK 21 or newer (tested with OpenJDK 25.0.2).
- Maven 3.9 (tested with 3.9.15); the repo has no Maven wrapper.
- A running Docker runtime (Docker Desktop; tested with 28.1.1), checked with
  `docker info`. `movies` and `recommendations` get their Postgres from Quarkus
  Dev Services.
- Ollama, tested with 0.35.1 and 0.40.0. `brew install ollama`
  currently installs 0.40.2, which was not tested, so run the checks in step 1.
- Network on first start: Maven dependencies, the `postgres:18` image, the
  239 MB MovieLens zip and the 2.5 GB model.

### 1. Ollama and the model

```sh
brew install ollama                                # macOS; or the app from https://ollama.com/download
ollama serve                                       # own terminal; skip if the Ollama app already runs
ollama pull qwen3:4b-instruct-2507-q4_K_M          # 2.5 GB, once
curl -s http://127.0.0.1:11434/api/version         # Ollama answers with its version
curl -s http://127.0.0.1:11434/api/tags            # must list qwen3:4b-instruct-2507-q4_K_M
```

> Pull the full tag. The bare `qwen3:4b` tag is the thinking build and much
> slower; `ai` uses the tag in `ai.model`.
>
> Run Ollama natively. In Docker on macOS it gets no GPU and runs several times
> slower.

### 2. Start the services

One terminal each, from the repo root, in this order:

```sh
cd app/services/users           && mvn quarkus:dev   # :8081
cd app/services/ai              && mvn quarkus:dev   # :8084
cd app/services/movies          && mvn quarkus:dev   # :8082
cd app/services/recommendations && mvn quarkus:dev   # :8083, start last
```

> Start `recommendations` last, once `users`, `ai` and `movies` have each logged
> `started in`. Its background agent runs once at startup and then every 10
> minutes (`recs.agent.every`). If the others are not up yet, it logs
> `agent run skipped users unavailable` and the cached lists stay empty until the
> next run. Restart `recommendations`, or start it with
> `RECS_AGENT_EVERY=30s mvn quarkus:dev` for a shorter interval.

> On its first start, `movies` downloads MovieLens `ml-32m` (239 MB, 87,585
> movies, ratings up to October 2023) into the JVM temp dir, never into the
> repo, so it is never committed (GroupLens license). Later starts reuse the
> cached zip. It seeds `movies_db` and opens its port only after the seed,
> which counts 32M ratings and takes several seconds.

> Dev Services removes the Postgres containers when a service stops. Each
> restart therefore re-seeds `movies_db` and empties `recs_db` until the agent's
> next run.

### 3. Check it works

```sh
curl -s -X POST -H 'Content-Type: application/json' \
  -d '{"text":"a horror movie from the 80s"}' \
  http://127.0.0.1:8083/recommendations/users/1/search   # calls ai, movies and Ollama; a few seconds on an Apple-silicon Mac
curl -s http://127.0.0.1:8083/recommendations/users/1    # reads recs_db only; filled by the agent's first run
```

The search answers the picks plus the filters the AI extracted:

```json
{"results":[{"movieId":"1258","title":"Shining, The (1980)"},{"movieId":"1994","title":"Poltergeist (1982)"},{"movieId":"1974","title":"Friday the 13th (1980)"}],"appliedFilters":{"genres":["Horror"],"yearFrom":1980,"yearTo":1989}}
```

The cached list is a plain list:

```json
[{"movieId":"1258","title":"Shining, The (1980)"},{"movieId":"1994","title":"Poltergeist (1982)"},{"movieId":"1974","title":"Friday the 13th (1980)"}]
```

- Users `1`, `2` and `3` come from the `users` stub
  (`curl -s http://127.0.0.1:8081/users`). Any other id gets `[]`.
- An HTTP 503 `{"error":"ai_unavailable","message":"AI service down"}` means
  `ai` could not use Ollama (not running, or the model is missing) or `movies`
  failed. Rerun the step 1 checks.

### Known limitations

- The catalog holds only titles, years and genres. MovieLens `ml-32m` has no
  cast, director or plot data, and its ratings end in October 2023.
- For themes outside the 19 genres ("cooking movies", "films about chess"),
  `ai` suggests film titles from the model's own knowledge and `movies` keeps
  the ones in the catalog. Invented titles are dropped, but the 4B model often
  misses obvious films (for "cooking movies" it skips Ratatouille and Julie &
  Julia, both in the catalog), so some picks fall back to the most-rated films.
- Requests about actors or directors are unreliable. For "movies with Brad
  Pitt" the model suggested The Revenant and Sicario next to real Brad Pitt
  films, and nothing in the catalog can reject a real film with the wrong cast.
  A fix would load cast and directors per film at seed time (for example from
  Wikidata, through the IMDb ids in MovieLens `links.csv`) and filter on them
  like genres.

Running Ollama in a cluster: see [deploy/ollama.md](deploy/ollama.md).

## Current state

Done:

- Maven reactor with five Quarkus modules, building and running.
- `edge` fan-out: both upstreams called concurrently.
- `ai` wraps Ollama: `POST /extract-filters` and `POST /rank` with the pinned
  `qwen3:4b-instruct-2507-q4_K_M`.
- `movies` seeds MovieLens `ml-32m` into `movies_db` and serves
  `GET /movies/genres`, `GET /movies/candidates` and `GET /movies/lookup`
  (catalog rows for the film titles `ai` suggests).
- `recommendations` runs the live search
  (`POST /recommendations/users/{id}/search`) and a background agent that caches
  each stub user's picks in `recs_db` (`GET /recommendations/users/{id}`).
- `users` stub: `GET /users` lists three fixed users.

Not started:

- `users` domain logic and persistence (`users_db`), registration and login.
- Web UI — framework undecided.
- Contract tests. `edge` holds its own copies of the `users` and
  `recommendations` response records, and nothing catches drift between them: a
  rename on one side surfaces at runtime as a null field, never at compile time.
- Resilience on the `edge` clients. No retry, timeout or circuit breaker —
  an upstream failure propagates straight to the caller.
- `compose.yaml` for the local container deployment.
- Terraform + Ansible cluster provisioning, and the app's Kubernetes manifests.
- Prometheus + Grafana, and the throughput/latency study.
- GitLab CI + Argo CD.

## Setup instructions

TODO — required by the spec: full instructions for bringing the system up from
a clean machine, for both the Docker Compose deployment and the Kubernetes one.

For the AI and recommendations slice in dev mode, see
*AI and recommendations slice: run locally* above.
