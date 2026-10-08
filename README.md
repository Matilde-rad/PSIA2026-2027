# g2 — AI-powered Movie Recommendation Hub

PSIA 2026/27 project, group 2.

> **Status: skeleton.** The services return hardcoded values. No database, no
> inference engine, no frontend, no deployment. See *Current state* below.

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
  edge/               aggregates services for the UI (no database)
frontend/web/       web UI — framework not chosen yet         (empty)
infra/terraform/    VMs for the cluster                       (empty)
infra/ansible/      installs software/configs onto those VMs  (empty)
deploy/             Kubernetes manifests, monitoring          (empty)
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

The four modules are independent no shared module, so no `-am` needed.

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
  secrets are needed.

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
- **Tests:** the pair in `src/test/resources` is test-only and public.
- **Before committing**, run `git status` and make sure no `.pem` other than the two
  `test-*.pem` files appears. `*.pem` and `app/keys/dev/` are gitignored as a safety net.
- Do not commit token-level debug logging of the JWT validator: it prints token details.

## Run locally (dev mode)

Dev ports are distinct so everything can run at once:

```sh
cd app/services/users           && mvn quarkus:dev   # :8081
cd app/services/movies          && mvn quarkus:dev   # :8082
cd app/services/recommendations && mvn quarkus:dev   # :8083
cd app/edge                     && mvn quarkus:dev   # :8080
```

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
token sent to them gets a 401. `edge`'s `/screens/profile/{id}` gets a 401 from
`users` until `edge` forwards the token (Phase 4).

```sh
curl http://127.0.0.1:8082/movies/42
curl http://127.0.0.1:8083/recommendations/users/42
curl http://127.0.0.1:8080/screens/profile/42
```

`edge` calls `users` and `recommendations` concurrently via
`Uni.combine().all().unis(...)`.

The three services expose `/q/health`, `/q/openapi` and `/q/swagger-ui` (the
last in dev only). `edge` carries neither the health nor the OpenAPI
extension yet, so it has none of them.

## Current state

Done:

- Maven reactor with four Quarkus modules, building and running.
- `edge` fan-out: both upstreams called concurrently.
- `users`: registration and login (RS256 JWT), `GET /users/me`, every other
  endpoint denied without a valid token (deny-by-default).

Not started:

- Domain logic and persistence for the other services (one Postgres per service).
- Inference engine (Ollama) and the background recommendation agent.
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
