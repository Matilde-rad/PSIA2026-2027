# g2 — AI-powered Movie Recommendation Hub

PSIA 2026/27 project, group 2.

> **Status:** registration, login and the web UI run locally, and so does the AI
> and recommendations slice (`ai`, `movies`, `recommendations`; see *AI and
> recommendations slice: run locally*). Profiles, follows and social discovery work
> end to end; the deployment is not built yet. See *Current state* below.

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
  services/ai/        wraps the Ollama embedding model (no database)
  edge/               aggregates services for the UI (no database)
frontend/web/       web UI (React + Vite), talks only to edge
scripts/            demo data seeding (just seed)
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

Profiles live in `users` too (`/profiles`). A profile has the same id as its
account; anyone logged in can read profiles, only the owner can create, change
or delete theirs (403 otherwise):

```sh
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Alice","bio":"likes films","favoriteGenres":["Horror"],"favoriteDirectors":["Ridley Scott"],"favoriteMovies":["1214"]}' \
  http://127.0.0.1:8081/profiles                                             # the caller's own profile
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8081/profiles/$ID   # ID = user.id from login
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8081/profiles?genre=horror&director=scott'
```

`PUT /profiles/{id}` creates the profile on the first save and replaces it
afterwards; `DELETE /profiles/{id}` removes it. The `genre` and `director`
filters are case-insensitive substring matches, done in Postgres.
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
  every watched movie with `userId`, `movieId`, `liked` and `watchedAt`. Nothing
  reads it yet: it is there for the recommendations agent to learn from.

`GET /internal/profiles` returns every profile to other services. It takes no
user token; the caller sends the shared key in `X-Internal-Key` instead
(`users.internal.key`, fixed to `dev-internal-key` in dev). In prod set
`USERS_INTERNAL_KEY` in `users` and the same value as `RECS_USERS_INTERNAL_KEY`
in `recommendations`, both from a Secret; there is no default.

```sh
curl http://127.0.0.1:8082/movies/embeddings
curl http://127.0.0.1:8082/movies/genres          # every genre in the catalog
curl 'http://127.0.0.1:8082/movies/search?q=par'  # title autocomplete, most rated first
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
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/profile/$ID
curl -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"a horror movie from the 80s"}' http://127.0.0.1:8080/screens/search
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/screens/discover?genre=horror'
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Alice","favoriteGenres":["Horror"],"favoriteMovies":["1214"]}' \
  http://127.0.0.1:8080/me/profile
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/social
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/screens/social/search?q=ali'
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/screens/profile/$ID/followers   # also /following
curl -H "Authorization: Bearer $TOKEN" 'http://127.0.0.1:8080/screens/movies/search?q=par'
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
- `/screens/discover` forwards its `genre` and `director` filters to `GET /profiles`
  in `users`.
- `/screens/genres` returns the catalog's genres from `movies` (`GET /movies/genres`);
  the profile form offers exactly these, so `movies` must be running to pick genres.

Web UI: with `users`, `recommendations` and `edge` running (plus `ai`, `movies`
and Ollama for the Search tab), run
`cd frontend/web && npm install && npm run dev`, then open http://127.0.0.1:5173.
Details in `frontend/web/README.md`.

`users`, `movies` and `recommendations` expose `/q/health`, `/q/openapi` and
`/q/swagger-ui` (the last in dev only).
`ai` and `edge` carry neither the health nor the OpenAPI extension, so they have
none of them. `users` also exposes Prometheus metrics at `/q/metrics` (Micrometer).

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
- Ollama, tested with 0.40.0. `brew install ollama`
- Git LFS (`brew install git-lfs`, then `git lfs install` once), tested with
  3.8.0. The precomputed movie vectors are a 1.2 GB LFS file; without LFS the
  clone holds only a pointer and the search finds nothing.
- Network on first start: Maven dependencies, the `pgvector/pgvector:pg18` image,
  the 239 MB MovieLens zip and the 639 MB embedding model.

### 1. Ollama, the model and the vectors

```sh
brew install ollama                                # macOS; or the app from https://ollama.com/download
ollama serve                                       # own terminal; skip if the Ollama app already runs
ollama pull qwen3-embedding:0.6b                   # 639 MB, once
curl -s http://127.0.0.1:11434/api/tags            # must list qwen3-embedding:0.6b
git lfs pull                                       # data/embeddings/*.csv, if the clone has only the pointer
```

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
> minutes (`recs.agent.every`). Each run reads every profile from `users`
> (`GET /internal/profiles`), turns each one into a search request ("Horror,
> Sci-Fi movies. Directed by Ridley Scott. Similar to Alien. ..."), searches once
> per distinct taste and caches 12 picks per user in `recs_db`. Favorite movies
> are ids in the profile; the agent gets their titles from `movies` in one lookup
> and leaves those movies out of the user's picks. Without `users` or `movies` a
> run logs `agent run skipped ... unavailable` and keeps the cache.

> On its first start, `movies` downloads MovieLens `ml-32m` (239 MB, 87,585
> movies, ratings up to October 2023) into the JVM temp dir, never into the
> repo. Later starts reuse the cached zip. It seeds `movies_db` and opens its
> port only after the seed, which counts 32M ratings and takes several seconds.
>
> Then, in the background, it loads one vector per movie from
> `data/embeddings/movielens_qwen3_embedding_0.6b.csv` and builds the HNSW
> index, about 1.5 min on an M4. `curl -s http://127.0.0.1:8082/movies/embeddings`
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

## Current state

Done:

- Maven reactor with five Quarkus modules, building and running.
- `edge` is the UI's backend for register, login, me and the profile screen: it
  verifies the JWT, forwards the caller's token, and on the caller's own screen calls
  `users` and `recommendations` concurrently.
- Contract fixtures in `app/contracts` catch field drift between `users` and `edge`.
- `users`: registration and login (RS256 JWT), `GET /users/me`, every other
  endpoint denied without a valid token (deny-by-default).
- Web UI in `frontend/web` (React + Vite): register, login, logout, a welcome
  page after registering that asks for the profile, and the profile page with
  real data and editing, all through `edge`. Home's For you tab shows the cached
  picks; its Search tab asks the AI (Ask AI) and shows 12 movies with posters.
  Favorite movies are picked with title autocomplete and shown as posters. The
  Social tab suggests people with your taste and searches by name; you can follow
  people and open anyone's followers and following.
- Profile CRUD (`/profiles`) in `users`, persisted in `users_db`, owner-only writes.
- Social discovery: `GET /profiles?genre=&director=` filters profiles by favourite
  genre or director, exposed as `edge`'s `/screens/discover` (the UI does not use
  it yet).
- Follows, follower lists, people suggestions by shared taste and people search
  (`/social` in `users`, `follows` table in `users_db`).
- `movies` serves title autocomplete (`GET /movies/search`), lookup by id
  (`POST /movies/lookup`) and the genre list (`GET /movies/genres`).
- Movie pages: `GET /movies/{id}` with synopsis, running time, cast, directors and
  IMDb rating from Stremio Cinemeta.
- Watchlist (`/watchlist` in `users`): to watch, then liked or disliked. The Feed
  tab shows what followed people did, a profile its owner's recent activity.
  Watched movies and verdicts are exposed at `GET /internal/watched`; the
  recommendations agent does not use them yet.
- `ai` wraps Ollama: `POST /embed/query` embeds a request with
  `qwen3-embedding:0.6b`.
- `movies` seeds MovieLens `ml-32m` into `movies_db`, loads the precomputed
  movie vectors into pgvector and serves `POST /movies/similar` (the nearest
  movies to a vector). Every row carries poster and banner image URLs.
- `recommendations` runs the live search
  (`POST /recommendations/users/{id}/search`) and serves the cached picks
  (`GET /recommendations/users/{id}`) from `recs_db`.
- The background agent fills that cache from the users' profiles every 10
  minutes, and a profile save refreshes its owner's picks at once.

Not started:

- Authentication on `recommendations`, `movies` and `ai`: they trust any caller,
  so they must not be reachable from outside the cluster.
- Resilience on the `edge` clients. No retry, timeout or circuit breaker —
  an upstream failure propagates straight to the caller.
- Terraform + Ansible cluster provisioning, and the app's Kubernetes manifests.
- Prometheus + Grafana, and the throughput/latency study.
- GitLab CI + Argo CD.

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

TODO — required by the spec: the Kubernetes deployment instructions.

For the AI and recommendations slice in dev mode, see
*AI and recommendations slice: run locally* above.
