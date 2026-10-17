# g2 — AI-powered Movie Recommendation Hub

PSIA 2026/27 project, group 2.

Users register, create a profile describing their interests, and ask the
platform's AI for movie recommendations matching that profile. They follow other
users, browse their profiles and activity, and keep a watchlist of movies to watch,
liked and disliked. A background agent proactively generates and caches
recommendations so they are ready at login.

## Structure

```
app/                Maven reactor (Java 21, Quarkus 3.39.4)
  services/users/     accounts, login, profiles, follows, watchlist  → users_db
  services/movies/    movie catalog and movie vectors (pgvector)     → movies_db
  services/recommendations/  AI recommendations + agent + cache      → recs_db
  services/ai/        wraps the Ollama embedding model (no database)
  edge/               backend-for-frontend for the UI (no database)
  contracts/          JSON fixtures shared by users and edge tests
  keys/               dev JWT key script (keys land in keys/dev/, git-ignored)
frontend/web/       web UI (React + Vite, served by nginx), talks only to edge
data/embeddings/    precomputed movie vectors (Git LFS, 1.2 GB)
scripts/            demo data seeding (just seed)
compose.yaml        the whole stack on one machine (just up)
infra/terraform/    GCP VMs for the cluster and the build VM
infra/ansible/      Kubernetes (kubeadm), registry, deploy, monitoring, CI runner, Argo CD
deploy/             Kubernetes manifests (kustomize), Argo CD app, Grafana dashboards
.gitlab-ci.yml      CI/CD pipeline: test, package, deploy
justfile            shortcuts for the commands below
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

The five modules are independent (no shared module), so no `-am` is needed.

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

Only `%dev.quarkus.http.port` differs; the packaged/container port is 8080
everywhere.

> If a Docker container already publishes `*:8080`, `http://localhost:8080` may
> resolve to it over IPv6 while Quarkus binds `127.0.0.1:8080`. The examples
> therefore use `http://127.0.0.1`.

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

Profiles live in `users` too (`/profiles`). A profile has the same id as its
account; anyone logged in can read profiles, only the owner can create, change
or delete theirs (403 otherwise):

```sh
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Alice","bio":"likes films","favoriteGenres":["Horror"],"favoriteDirectors":["Ridley Scott"],"favoriteMovies":["1214"]}' \
  http://127.0.0.1:8081/profiles/$ID                                         # ID = user.id from login
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/profiles/$ID
```

`PUT /profiles/{id}` creates the profile on the first save and replaces it
afterwards; `DELETE /profiles/{id}` removes it.
`favoriteMovies` holds ids of the `movies` catalog (1214 is Alien), not titles;
a profile takes at most 10 movies, 10 directors and 30 genres.

Follows and people are under `/social`, also in `users`, always from the
caller's point of view:

```sh
curl -X PUT    -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/social/following/$OTHER   # follow, 204
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/social/following/$OTHER   # unfollow, 204
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/social/$OTHER/stats       # followers, following, followedByMe
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/social/$OTHER/followers   # also /following
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/social/suggestions
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8081/social/search?q=ali'
```

- Suggestions are the people the caller does not follow yet whose profile
  overlaps with theirs, closest first: one point per shared genre or director,
  two per shared favorite movie. No profile, no suggestions.
- Each person comes with `followedByMe` and the genres, directors and movie ids
  they share with the caller.
- Search matches the username or the display name, at least 2 characters.

Each user's movies are under `/watchlist`, also in `users` (`user_movies` table). A
movie is `to_watch`, or watched with a verdict: `liked` or `disliked`:

```sh
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"to_watch"}' http://127.0.0.1:8081/watchlist/1214      # then "liked" or "disliked"
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/watchlist            # mine, newest first
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/watchlist/1214
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/activity/feed        # people I follow
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/activity/users/$OTHER
```

- There is one row per user and movie, so an activity is the latest state of that
  pair: a movie that goes from `to_watch` to `liked` shows once, as liked.
- `GET /internal/watched` (same shared key as `/internal/profiles`, below) returns
  every watched movie with `userId`, `movieId`, `liked` and `watchedAt`. The
  recommendations agent reads it to learn from likes and dislikes.

`GET /internal/profiles` returns every profile to other services. It takes no
user token; the caller sends the shared key in `X-Internal-Key` instead
(`users.internal.key`, fixed to `dev-internal-key` in dev). In prod set
`USERS_INTERNAL_KEY` in `users` and the same value as `RECS_USERS_INTERNAL_KEY`
in `recommendations`, both from a Secret; there is no default.

```sh
curl http://127.0.0.1:8082/movies/embeddings
curl http://127.0.0.1:8082/movies/genres          # every genre in the catalog
curl 'http://127.0.0.1:8082/movies/search?q=par'  # title autocomplete, most rated first
curl 'http://127.0.0.1:8082/movies/browse?q=matrix&genre=Sci-Fi&offset=0'  # Search tab: title words and genres (all must match), a page at a time
curl -X POST -H 'Content-Type: application/json' -d '{"ids":["1214","2571"]}' \
  http://127.0.0.1:8082/movies/lookup             # movies by id, with poster URLs
curl http://127.0.0.1:8082/movies/1214            # one movie: synopsis, runtime, cast, directors, IMDb rating
curl http://127.0.0.1:8083/recommendations/users/42
```

`GET /movies/{id}` adds what MovieLens lacks from Stremio's Cinemeta addon
(`v3-cinemeta.strem.io/meta/movie/<imdb id>.json`, no API key). It is asked on a
movie's first request and the answer kept in `movies_db` (`movie_meta`). If
Cinemeta does not know the film or cannot be reached, the movie still comes back,
with those fields null or empty.

The UI talks only to `edge` (:8080, start `users` and `recommendations` too):

```sh
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' http://127.0.0.1:8080/auth/register
curl -X POST -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password1"}' http://127.0.0.1:8080/auth/login   # token and user.id
TOKEN=<the token from the login response>
ID=<user.id from the login response>
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/me
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/home
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/profile/$ID
curl -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"a horror movie from the 80s"}' http://127.0.0.1:8080/screens/search
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Alice","favoriteGenres":["Horror"],"favoriteMovies":["1214"]}' \
  http://127.0.0.1:8080/me/profile
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/social
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/screens/social/search?q=ali'
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/profile/$ID/followers   # also /following
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/screens/movies/search?q=par'
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/screens/browse?q=matrix&genre=Sci-Fi&genre=Comedy&offset=12'
curl -X PUT -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/me/following/$OTHER      # DELETE unfollows
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/movie/1214
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"to_watch"}' http://127.0.0.1:8080/me/watchlist/1214                          # DELETE removes
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/watchlist
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/feed
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/profile/$ID/activity
```

- Register and login are the only public routes; everything else needs the token.
  `edge` verifies it and forwards it to `users`.
- Your own screen has `recommendations`; any other user's screen has
  `"recommendations": []`. Every screen has `profile`, which is `null` until
  that user saves one, `favoriteMovies` (the profile's movie ids as movies with
  titles and posters, from `movies`), `followers`, `following` and `followedByMe`.
- `PUT /me/profile` saves the caller's profile in `users`, then asks
  `recommendations` to recompute their picks from it, so Home is ready right away.
  If that recompute fails or takes longer than 15 s the profile is still saved and
  the background agent catches up on its next run.
- Every error has the `users` JSON shape. A 400, 404, 409 or 401 from `users` comes back
  unchanged.
- A failing or unreachable `users` or `recommendations` gives 502 `bad_gateway`. A
  malformed id gives 404.
- `/screens/search` asks the AI for 12 movies through `recommendations`, so it also
  needs `ai`, `movies` and Ollama (see *AI and recommendations slice: run locally*).
  When the AI cannot answer it gives 503 `ai_unavailable`.
- `/screens/search` also takes `exclude` (movie ids already shown) and
  `POST /screens/home/more` with `{"exclude": [...]}` returns the next For you picks
  from the caller's profile; neither writes the recommendation cache.
- `/screens/browse` is Home's Search tab, with no AI. It returns 12 catalog movies
  whose title has every typed word and that have every chosen `genre`, from `offset`.
  With text, titles that start with it come first; otherwise the most rated come first.
- A profile screen calls `users` three times (account, profile, follow counts)
  and, on your own screen, `recommendations`, all concurrently via
  `Uni.combine()`, then `movies` once for the favorite movies. It needs `movies`
  whenever the profile has favorite movies: without it the screen is a 502 rather
  than a profile that seems to have none.
- `/screens/social` returns people with the caller's taste, `/screens/social/search`
  people by name. Shared movies come back as titles: one `movies` lookup for the
  whole list.
- `/screens/movie/ID` is the movie page: the details from `movies` plus `status`, the
  caller's own (`to_watch`, `liked`, `disliked` or `null`), from `users`.
- `/screens/watchlist` returns the caller's movies as `toWatch` and `watched`, with
  titles and posters from one `movies` lookup. `/screens/feed` (people the caller
  follows) and `/screens/profile/ID/activity` (one user) do the same for activity.
- `/screens/movies/search` is the title autocomplete of the profile form.
- `/screens/genres` returns the catalog's genres from `movies` (`GET /movies/genres`);
  the profile form offers exactly these, so `movies` must be running to pick genres.

Web UI: with `users`, `recommendations` and `edge` running (plus `ai`, `movies`
and Ollama for the Search tab), run
`cd frontend/web && npm install && npm run dev`, then open http://127.0.0.1:5173.
Details in `frontend/web/README.md`.

All five services expose `/q/health` and Prometheus metrics at `/q/metrics`
(Micrometer, with latency histograms for incoming and outgoing HTTP calls).
`users`, `movies` and `recommendations` also expose `/q/openapi` and
`/q/swagger-ui` (the last in dev only).

## AI and recommendations slice: run locally

This starts `ai`, `movies` and `recommendations` in dev mode against a local
Ollama. `users` and `edge` are not needed for the checks below. The timings
quoted are from an Apple M4 with 16 GB.

### Prerequisites

- JDK 21 or newer.
- Maven 3.9; the repo has no Maven wrapper.
- A running Docker runtime, checked with `docker info`. `movies` and
  `recommendations` get their Postgres from Quarkus Dev Services.
- Ollama (https://ollama.com/download).
- Git LFS (from your package manager, then `git lfs install` once). The
  precomputed movie vectors are a 1.2 GB LFS file; without LFS the clone holds
  only a pointer and the search finds nothing.
- Network on first start: Maven dependencies, the `pgvector/pgvector:pg18` image,
  the 239 MB MovieLens zip and the 639 MB embedding model.

### 1. Ollama, the model and the vectors

```sh
ollama serve                                       # own terminal; skip if the Ollama app already runs
ollama pull qwen3-embedding:0.6b                   # 639 MB, once
curl -s http://127.0.0.1:11434/api/tags            # must list qwen3-embedding:0.6b
git lfs pull                                       # data/embeddings/*.csv, if the clone has only the pointer
```

> On macOS, run Ollama natively: in Docker it gets no GPU and runs several times
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
> minutes (`recs.agent.every`). Each run reads every profile from `users`
> (`GET /internal/profiles`), turns each one into a search request ("Horror,
> Sci-Fi movies. Directed by Ridley Scott. Similar to Alien. ..."), searches once
> per distinct taste and caches each user's best 24 matches
> (`recs.recommend.pool`) in `recs_db`; Home draws 12 of them at random on every
> visit. A user whose taste has not changed since their pool was cached is not
> searched again until the pool is 24 hours old (`recs.agent.max-age`), and the
> run log counts them as `unchanged`. Favorite movies are ids in the profile;
> the agent gets their titles from `movies` in one lookup. It also reads the
> watched movies (`GET /internal/watched`): the search moves towards the
> vectors of liked movies and away from disliked ones
> (`recs.recommend.like-weight`, `dislike-weight`). Favorite and watched movies
> are left out of the user's picks. Without `users` or `movies` a run logs
> `agent run skipped ... unavailable` and keeps the cache.

> On its first start, `movies` downloads MovieLens `ml-32m` (239 MB, 87,585
> movies, ratings up to October 2023) into the JVM temp dir, never into the
> repo. Later starts reuse the cached zip. It seeds `movies_db` and opens its
> port only after the seed, which counts 32M ratings and takes several seconds.
>
> Then, in the background, it loads one vector per movie from
> `data/embeddings/movielens_qwen3_embedding_0.6b.csv` and builds the HNSW
> index, about 1.5 min. `curl -s http://127.0.0.1:8082/movies/embeddings`
> shows the count (87,585 when done). Searches made before that find fewer or no
> movies.

> Dev Services removes the Postgres containers when a service stops. Each
> restart therefore re-seeds `movies_db` and empties `recs_db` until the agent's
> next run.

### 3. Check it works

```sh
curl -s -X POST -H 'Content-Type: application/json' \
  -d '{"text":"a horror movie from the 80s"}' \
  http://127.0.0.1:8083/recommendations/users/1/search   # calls ai, movies and Ollama; under 1 s on an M4
curl -s http://127.0.0.1:8083/recommendations/users/1    # reads recs_db only; [] until that user has a profile
```

The search answers 12 picks (shortened here to the first):

```json
{"results":[{"movieId":"1994","title":"Poltergeist (1982)","posterUrl":"https://images.metahub.space/poster/medium/tt0084516/img","bannerUrl":"https://images.metahub.space/background/medium/tt0084516/img"}]}
```

How it picks: `ai` embeds the request with `qwen3-embedding:0.6b`, `movies` returns
the 60 nearest movies with at least 50 ratings (pgvector, cosine distance), and
`recommendations` keeps the 12 best by similarity plus a small popularity bonus.
No LLM is involved.

The cached list, once the agent fills it, is a plain list of the same picks.
`POST /recommendations/users/{id}/refresh` with a profile as the body fills it
for one user at once; `edge` calls it after a profile save.

- Poster and banner images come from Stremio MetaHub
  (`images.metahub.space`), addressed by the IMDb id in MovieLens `links.csv`. No
  API key is needed. A film MetaHub has no image for gets a 404, and the UI shows
  its placeholder card instead.

- The search works for any user id; it does not call `users`.
- An HTTP 503 `{"error":"ai_unavailable","message":"AI service down"}` means
  `ai` could not use Ollama (not running, or the model is missing) or `movies`
  failed. Rerun the step 1 checks.
- After changing the embedding model or the movie text, the vectors in
  `data/embeddings` must be recomputed with the same model: a query and the
  movie vectors from different models do not match.

### Known limitations

- Each movie's vector comes from its title, year, genres and up to 15 MovieLens
  user tags. MovieLens `ml-32m` has no cast, director or plot data, so requests
  about actors work only where tags name them, and its ratings end in October
  2023.
- There are no hard filters: "80s horror" ranks 80s horror films high, but a
  close match from another decade can still appear.
- Most of the 87k movies have no tags; their vectors rest on the title alone,
  which is why the search skips movies with fewer than 50 ratings
  (`recs.semantic.min-ratings`).

Running Ollama in a cluster: see [deploy/ollama.md](deploy/ollama.md).

## Monitoring

Prometheus and Grafana (kube-prometheus-stack) run in the `monitoring` namespace,
installed by `infra/ansible/7-monitoring.yml`. A PodMonitor scrapes `/q/metrics` of
the five services. The *Plain Sight / Services* and *Plain Sight / Load Test*
dashboards live in `deploy/monitoring/dashboards/` and are loaded on every run of
the playbook. Grafana is at `http://<worker1 public IP>:30300`, user `admin`, with
the password in `infra/secrets/grafana-admin-password`.

## CI/CD

GitLab CI tests and packages every commit; Argo CD deploys `master` (GitOps).

```
push ─▶ test ──────────────────▶ package ───────────────▶ deploy (master only)
        test:service ×5            build 6 images,           write the tag into
        (mvn verify, Postgres      tag = commit SHA,         deploy/kustomization.yaml,
         via Dev Services)         push to the registry      commit [skip ci], push
        test:web (vite build)      on the build VM                  │
                                                                    ▼
                                         Argo CD sees the commit, syncs deploy/ to g2
```

- `.gitlab-ci.yml` runs on our own runner on the build VM (`infra/ansible/8-ci-runner.yml`).
  It needs Docker for the tests and is the only machine that can push to the
  internal registry.
- Each service is tested on its own, and its tested `target/quarkus-app` is handed
  to `package`, so the images hold exactly what passed (`deploy/images.md`).
- Pipelines run on `master`, on merge requests, and from *Run pipeline* in GitLab.
  A push to any other branch runs nothing until it has a merge request.
- `deploy` only runs on `master`. It commits the new tag with `[skip ci]`, so the bot
  commit starts no pipeline. If pipelines finish out of order, it does not replace a
  newer tag with an older one.
- Argo CD (`infra/ansible/9-argocd.yml`) watches `deploy/` on `master` with automatic
  sync and self-heal: git is what runs. UI at `http://<worker1 public IP>:30880`.
- Rolling back is a revert of the bot commit, or of the change itself.

Setup (tokens, playbooks): *CI/CD (steps 8 and 9)* in `infra/ansible/README.md`.

## Setup instructions

### Run with Docker Compose

Prerequisites: Docker Desktop running, JDK 21+, Maven, Node and
[`just`](https://github.com/casey/just), `git lfs pull` (`data/embeddings` holds
the 1.2 GB vector file), and `./app/keys/gen-dev-keys.sh` once.

```bash
just up
```

`just up` compiles the services and the web app on this machine
(`just build`), then runs `docker compose up --build`. The Dockerfiles only
package what was compiled; see [deploy/images.md](deploy/images.md).

Then open http://127.0.0.1:8000, register, log in and search.

The first start takes a few minutes: Ollama pulls `qwen3-embedding:0.6b`,
`movies` downloads and seeds MovieLens, and then the vectors load in the
background. Search returns nothing until loading finishes;
`docker compose exec edge curl -s http://movies:8080/movies/embeddings` shows
the progress up to 87585. Later starts reuse the seeded databases, the cached
MovieLens zip and the pulled model.

Only port 8000 is published, on 127.0.0.1, so the stack can run next to dev
mode. Set `INTERNAL_KEY` to override the key that `users` and `recommendations`
share. `just down` stops the stack and keeps the data; `just reset` also wipes it.

`just seed` (after the first start has finished seeding the catalog) creates about 12
demo users through edge with profiles, follows and watchlist entries, all with password
`demo1234`; `just seed http://127.0.0.1:8080` targets edge in dev mode. All their
activity has the time the script ran. Local stacks only.

### Deploy to Kubernetes (GCP)

The cluster is self-hosted: four VMs (`master`, `worker1` .. `worker3`) with
kubeadm, plus a `build` VM that builds the images and holds the registry. All
tools (Terraform, Ansible, gcloud, kubectl) run in a management container, so the
host only needs Docker, `just` and `ssh-keygen`.

1. **VMs.** Follow the one-time setup in
   [infra/terraform/README.md](infra/terraform/README.md) (GCP project, service
   account key in `infra/secrets/gcp-key.json`, `just keys`, project ID), then:

   ```sh
   just mgmt-start && just mgmt          # shell in the management container, repo at /g2
   cd infra/terraform && terraform init && terraform apply
   ```

2. **GitLab tokens.** Create the runner, deploy and push tokens described in
   *CI/CD (steps 8 and 9)* of [infra/ansible/README.md](infra/ansible/README.md).

3. **Cluster, images, app, monitoring and CI/CD.** Still in the container:

   ```sh
   cd /g2/infra/ansible
   python3 inventory.py                  # inventory from the Terraform outputs
   ansible-playbook site.yml             # playbooks 1-9 in order
   ```

   `site.yml` prepares the nodes, installs Kubernetes, builds and pushes the
   images, deploys the app, installs Prometheus and Grafana, the GitLab Runner and
   Argo CD. Each playbook can also be run on its own (see
   [infra/ansible/README.md](infra/ansible/README.md)).

The app is then at `http://<worker1 public IP>`. On the first deploy `movies`
seeds its database for several minutes; `kubectl -n g2 get pods -w` shows when
everything is `Ready`. What runs and why is in [deploy/README.md](deploy/README.md).
From then on every push to `master` is tested, packaged and deployed by Argo CD
(see *CI/CD* above).

Only `web` (port 80 of worker1) is reachable from outside the cluster, besides the
Argo CD and Grafana UIs. `movies`, `recommendations`, `ai` and Ollama have no
authentication of their own and are ClusterIP Services only; `users` checks the JWT
on every request and the shared internal key on `/internal/*`.

Run `terraform destroy` inside the container when done, so the VMs stop using
credits.

For the AI and recommendations slice in dev mode, see
*AI and recommendations slice: run locally* above.
