# Container images

How the service images are built and why. Written for the report.

## Why a container needs a base image at all

A container shares only the kernel with its host. It does not see the host's
Java or files, so everything a service needs to run must be inside its image.
`quarkus-run.jar` is JVM bytecode, not something the OS can execute on its own,
so every Java service image starts `FROM eclipse-temurin` (the Java 21 JRE), a small
Ubuntu with only the Java runtime. The web image starts from `nginx`, which
serves the built files.

## Compile on the host, package in Docker

Compiling and packaging are two separate steps:

1. **`just build`** compiles on the developer's machine. One Maven run builds
   all five services (`mvn package` in `app/`), and `npm run build` builds the
   web app into `frontend/web/dist/`.
2. **The Dockerfiles** only package what step 1 produced. `app/Dockerfile`
   copies one service's `target/quarkus-app/` onto the JRE image.
   `frontend/web/Dockerfile` copies `dist/` and `nginx.conf` onto nginx.

`just up` runs both steps and then starts the stack with Docker Compose.

The same Dockerfile serves `users`, `movies`, `recommendations`, `ai` and
`edge`, selected with `--build-arg MODULE=<module path>`, so the five images
cannot drift apart.

### Why this split

- **The Dockerfiles stay short.** Each is a handful of lines with one stage,
  and it reads as "this runtime plus this app".
- **One build for all services.** A single Maven run compiles all five
  services and reuses the local dependency cache. Building inside Docker
  would run Maven five times, once per image.
- **The images hold the tested artifacts.** The jars that go into the images
  are the same ones the tests ran against. In CI the pipeline runs
  `mvn package` with the tests once, then builds and pushes the images from
  that output.

The cost is that the machine building the images needs JDK 21+, Maven and
Node, not only Docker. Anyone working on the project already has these to run
the services in dev mode. JDK 21+ is enough, because the build targets
Java 21 (`maven.compiler.release`).

### Alternatives considered

| Option | Why not |
|---|---|
| Multi-stage Dockerfile that also compiles (a `maven` build stage, then the JRE stage) | Works with Docker alone, but every Dockerfile carries a whole build stage, and the five images each run their own Maven build. We used this first and replaced it. |
| Quarkus native build with GraalVM, on a minimal base without Java | The build takes minutes per service and a lot of RAM, and some libraries need extra configuration. Faster startup does not matter for this project. |
| `FROM scratch` (an empty image) | Only works for fully static binaries; a JVM application cannot run on it. |
| One Dockerfile per service | Five copies of the same recipe to keep in sync. |

### Other choices in the Dockerfiles

- **Pinned versions.** Every base image has an exact tag, never `latest`, so a
  rebuild gives the same result.
- **Only build output goes to Docker.** The `.dockerignore` files list what to
  include, not what to exclude: `target/quarkus-app` per service, and `dist/`
  plus `nginx.conf` for the web. Sources and the dev JWT keys can never end
  up in an image.
- **No secrets in images.** The JWT keys, database credentials and the
  internal key are given at run time, through mounted files and environment
  variables.
- **Non-root user.** The service runs as `app` (uid 10001), so a compromised
  service does not get root inside its container.
- **Memory.** `-XX:MaxRAMPercentage=75` sizes the heap from the container's
  memory limit (set per service in `compose.yaml`), leaving the rest for the
  JVM's own non-heap memory.
- **Clean shutdown.** The `ENTRYPOINT` is in exec form, so `java` runs as
  process 1 and receives `docker stop` directly. Quarkus then shuts down
  cleanly and closes its database connections.
- **One origin for the browser.** nginx serves the single-page app and
  forwards `/auth`, `/me` and `/screens` to `edge`, so `edge` needs no CORS.
