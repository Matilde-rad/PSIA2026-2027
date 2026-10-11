# g2 — AI-powered Movie Recommendation Hub

PSIA 2026/27 project, group 2.

> **Status:** registration, login and the web UI run locally, and so does the AI
> and recommendations slice (`ai`, `movies`, `recommendations`; see *AI and
> recommendations slice: run locally*). Profiles, social discovery and the
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
frontend/web/       web UI (React + Vite), talks only to edge
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

## Running tests

```sh
cd app && mvn verify                   # all modules
mvn -f app/pom.xml verify              # same, from the repo root
mvn -f app/pom.xml -pl services/users test -Dtest=RegisterResourceTest   # one class
```

- A Docker-compatible container engine must be running and
  reachable from your shell. Check with `docker info` before running the tests.
- Quarkus Dev Services starts a throwaway Postgres container for the tests (the
  first run pulls the postgres image and testcontainers/ryuk), so no database
  has to be started by hand.
- If Docker is unreachable the build **fails**. There is deliberately no H2
  fallback and no skipping: H2 would hide Postgres behaviour such as the
  lowercase `CHECK` constraint on usernames.
- Tests use a random HTTP port, so `quarkus:dev` instances on 8080-8083 can stay
  running.
- The test profile uses bcrypt cost 4 (`%test.users.bcrypt.cost`); production
  keeps the default of 12.
- Tests sign and verify JWTs with the committed test-only key pair in
  `app/services/users/src/test/resources` (see `TEST-ONLY-KEYS.md` there), so no
  secrets are needed. `edge` has its own copy in `app/edge/src/test/resources`.
- The `edge` tests need no Docker: they stub `users` and `recommendations` with
  Mockito (`@InjectMock`) and sign tokens with edge's copy of the test-only pair.
- The JSON files in `app/contracts/` are the contract between `users` and `edge`.
  `ContractFixturesTest` in both modules fails the build if a field of the user or
  login response drifts.

## Auth keys (users)

The `users` service signs login tokens (RS256) with a private key and verifies them
with the matching public key. Only `users` ever holds the private key.

- **Dev:** run `./app/keys/gen-dev-keys.sh` once before `mvn quarkus:dev` in
  `app/services/users`. The keys land in `app/keys/dev/`, which is gitignored and
  outside every module's resources. The script does not overwrite existing keys unless you pass `--force`.
- **Prod:** set `SMALLRYE_JWT_SIGN_KEY_LOCATION` and
  `MP_JWT_VERIFY_PUBLICKEY_LOCATION` to files of a mounted secret, for example
  `file:/etc/jwt/privateKey.pem` and `file:/etc/jwt/publicKey.pem`. There is no
  fallback to the dev or test keys.
- **edge:** only verifies tokens, with the public key. Dev reads
  `app/keys/dev/publicKey.pem` (run `./app/keys/gen-dev-keys.sh` first); prod needs only
  `MP_JWT_VERIFY_PUBLICKEY_LOCATION`. edge never gets the private key.
- **Tests:** the pairs in `src/test/resources` (users and edge) are test-only and public.
- **Before committing**, run `git status` and make sure no `.pem` other than the four
  `test-*.pem` files (two in users, two in edge) appears. `*.pem` and `app/keys/dev/` are gitignored as a safety net.
- Do not commit token-level debug logging of the JWT validator: it prints token details.

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

For `users`, register, log in to get a token, then call a protected endpoint with it
(run `./app/keys/gen-dev-keys.sh` first, see *Auth keys (users)*):

```sh
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' http://127.0.0.1:8081/users
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' http://127.0.0.1:8081/auth/login   # returns "token"
TOKEN=<the token from the login response>
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/users/me
```

Every `users` endpoint except `POST /users` and `POST /auth/login` needs a token.
Call register and login **without** an `Authorization` header: a stale or invalid
token sent to them gets a 401.

```sh
curl http://127.0.0.1:8082/movies/genres
curl http://127.0.0.1:8083/recommendations/users/42
```

The UI talks only to `edge` (:8080, start `users` and `recommendations` too):

```sh
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' http://127.0.0.1:8080/auth/register
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' http://127.0.0.1:8080/auth/login   # token and user.id
TOKEN=<the token from the login response>
ID=<user.id from the login response>
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/me
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/profile/$ID
curl -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"a horror movie from the 80s"}' http://127.0.0.1:8080/screens/search
```

- Register and login are the only public routes; everything else needs the token.
  `edge` verifies it and forwards it to `users`.
- Your own screen has `recommendations`; any other user's screen has
  `"recommendations": []`.
- Every error has the `users` JSON shape. A 400, 404, 409 or 401 from `users` comes back
  unchanged.
- A failing or unreachable `users` or `recommendations` gives 502 `bad_gateway`. A
  malformed id gives 404.
- `/screens/search` asks the AI for 10 movies through `recommendations`, so it also
  needs `ai`, `movies` and Ollama (see *AI and recommendations slice: run locally*).
  When the AI cannot answer it gives 503 `ai_unavailable`.
- On your own screen `edge` calls `users` and `recommendations` concurrently via
  `Uni.combine()`.

Web UI: with `users`, `recommendations` and `edge` running (plus `ai`, `movies`
and Ollama for the Search tab), run
`cd frontend/web && npm install && npm run dev`, then open http://127.0.0.1:5173.
Details in `frontend/web/README.md`.

`users`, `movies` and `recommendations` expose `/q/health`, `/q/openapi` and
`/q/swagger-ui` (the last in dev only).
`ai` and `edge` carry neither the health nor the OpenAPI extension, so they have
none of them.

## AI and recommendations slice: run locally

This starts `ai`, `movies` and `recommendations` in dev mode against a local
Ollama. `users` and `edge` are not needed for the checks below. Tested on macOS
(Apple M4, 16 GB).

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
cd app/services/ai              && mvn quarkus:dev   # :8084
cd app/services/movies          && mvn quarkus:dev   # :8082
cd app/services/recommendations && mvn quarkus:dev   # :8083, start last
```

> Start `recommendations` last, once `ai` and `movies` have each logged
> `started in`. Its background agent runs once at startup and then every 10
> minutes (`recs.agent.every`). It still lists users through `GET /users`, which
> `users` no longer serves, so every run logs
> `agent run skipped users unavailable` and caches nothing until the agent reads
> user profiles.

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
curl -s http://127.0.0.1:8083/recommendations/users/1    # reads recs_db only; [] until the agent reads profiles
```

The search answers 10 picks plus the filters the AI extracted (shortened here to
the first pick):

```json
{"results":[{"movieId":"1347","title":"Nightmare on Elm Street, A (1984)","posterUrl":"https://images.metahub.space/poster/medium/tt0087800/img","bannerUrl":"https://images.metahub.space/background/medium/tt0087800/img"}],"appliedFilters":{"genres":["Horror"],"yearFrom":1980,"yearTo":1989}}
```

The cached list, once the agent fills it, is a plain list of the same picks.

- Poster and banner images come from Stremio MetaHub
  (`images.metahub.space`), addressed by the IMDb id in MovieLens `links.csv`. No
  API key is needed. A film MetaHub has no image for gets a 404, and the UI shows
  its placeholder card instead.

- The search works for any user id; it does not call `users`.
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
  With 10 picks this shows: "cooking movies" returns Chef and Food, Inc., then
  Toy Story, Star Wars and other most-rated films.
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
- `edge` is the UI's backend for register, login, me and the profile screen: it
  verifies the JWT, forwards the caller's token, and on the caller's own screen calls
  `users` and `recommendations` concurrently.
- Contract fixtures in `app/contracts` catch field drift between `users` and `edge`.
- `users`: registration and login (RS256 JWT), `GET /users/me`, every other
  endpoint denied without a valid token (deny-by-default).
- Web UI in `frontend/web` (React + Vite): register, login, logout and the profile
  header with real data through `edge`. Home's Search tab asks the AI (Ask AI) and
  shows 10 movies with posters. The genre bar, Edit profile and Social search are
  static previews until their services exist.
- `ai` wraps Ollama: `POST /extract-filters` and `POST /rank` with the pinned
  `qwen3:4b-instruct-2507-q4_K_M`.
- `movies` seeds MovieLens `ml-32m` into `movies_db` and serves
  `GET /movies/genres`, `GET /movies/candidates` and `GET /movies/lookup`
  (catalog rows for the film titles `ai` suggests). Every row carries poster and
  banner image URLs.
- `recommendations` runs the live search
  (`POST /recommendations/users/{id}/search`) and serves the cached picks
  (`GET /recommendations/users/{id}`) from `recs_db`.

Not started:

- Profiles and social discovery.
- The background agent's user source. It still calls `GET /users`, which
  `users` no longer has, so it caches nothing until it reads user profiles.
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
