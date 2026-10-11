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
  services/social-discovery/ browse/filter other users' profiles (no database)
  edge/               aggregates services for the UI (no database)
frontend/web/       web UI — framework not chosen yet         (empty)
infra/terraform/    VMs for the cluster                       (empty)
infra/ansible/      installs software/configs onto those VMs  (empty)
deploy/             Kubernetes manifests, monitoring          (empty)
```

Each microservice owns a dedicated database instance. `edge` is
the backend-for-frontend: an aggregator rather than a domain service, so it has
no database. `social-discovery` has none either: it reads profiles from `users`
over REST.

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
cd app/services/movies          && mvn quarkus:dev   # :8082
cd app/services/recommendations && mvn quarkus:dev   # :8083
cd app/services/social-discovery && mvn quarkus:dev  # :8084
cd app/edge                     && mvn quarkus:dev   # :8080
```

Only `%dev.quarkus.http.port` differs the packaged/container port is 8080
everywhere.

`users` has a Postgres datasource, so its dev mode needs Docker (or Podman)
running: Quarkus Dev Services starts the database container for it.

> A Docker container on this machine may already publish `*:8080`, in which case
> `http://localhost:8080` resolves to it over IPv6 while Quarkus binds
> `127.0.0.1:8080`. Use `http://127.0.0.1:8080`.

```sh
curl http://127.0.0.1:8081/users/42
curl http://127.0.0.1:8081/profiles
curl http://127.0.0.1:8082/movies/42
curl http://127.0.0.1:8083/recommendations/users/42
curl 'http://127.0.0.1:8084/discover/users?genre=horror'
curl http://127.0.0.1:8080/screens/profile/42
```

`edge` calls `users` and `recommendations` concurrently via
`Uni.combine().all().unis(...)`.

The four services expose `/q/health`, `/q/openapi` and `/q/swagger-ui` (the
last in dev only). `edge` carries neither the health nor the OpenAPI
extension yet, so it has none of them.

## Current state

Done:

- Maven reactor with five Quarkus modules, building and running.
- Profile CRUD (`/profiles`) in `users`, persisted in `users_db`.
- `social-discovery`: filter profiles by favourite genre or director.
- `edge` fan-out: both upstreams called concurrently.

Not started:

- Remaining domain logic and persistence (one Postgres per service),
  registration and login. `/users/{id}` is still hardcoded and not linked to
  the stored profiles.
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
