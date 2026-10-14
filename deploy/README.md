# deploy

Kubernetes manifests for the app, applied with kustomize by
`infra/ansible/6-deploy.yml` (`just deploy`). `images.md` explains how the container
images are built, and `ollama.md` gives the sizing and settings for the Ollama pod.

## What runs

Everything lives in the `g2` namespace, one replica each, mirroring `compose.yaml`:

| Component | Kind | File | Notes |
|---|---|---|---|
| `users-db`, `movies-db`, `recs-db` | StatefulSet + Service | `apps/<service>/*-db.yaml` | Postgres 18 (`movies-db` with pgvector), a `local-path` volume each |
| `ollama` | StatefulSet + Service | `apps/ai/ollama.yaml` | Pulls `qwen3-embedding:0.6b` into its volume once, ClusterIP only |
| `users`, `movies`, `recommendations` | Deployment + Service | `apps/<service>/<service>.yaml` | Probes on `/q/health/started`, `/ready`, `/live` |
| `ai`, `edge` | Deployment + Service | `apps/ai/ai.yaml`, `apps/edge/edge.yaml` | No health extension, so TCP probes on 8080 |
| `web` | Deployment | `apps/web/web.yaml` | nginx on port 80 of worker1 |

`kustomization.yaml` lists the files, puts them in `g2`, and maps each image name
(`image: users`) to `<registry>/<name>:<tag>`. The registry and tag in the file are
placeholders: `6-deploy.yml` fills in the build VM's registry and the git short SHA
in a temporary copy and applies that, so the file in git never changes on a deploy.

## Choices

- **Entry point: `web` on hostPort 80.** nginx already serves the React app and
  forwards `/auth`, `/me` and `/screens` to `edge`, so it is the app's front door.
  It is pinned to worker1 and takes port 80 of that node, so the app is at
  `http://<worker1 public IP>` (the firewall already allows 80). No ingress
  controller or Gateway is needed. Its strategy is `Recreate`, because two pods
  cannot hold the same host port during an update.
- **Storage: local-path-provisioner.** kubeadm has no StorageClass, and Rancher's
  local-path provisioner adds one (`local-path`) that makes a folder on the node's
  disk for each volume. The databases and Ollama keep their data across pod
  restarts. A pod stays on the node where its volume was made.
- **The embeddings CSV: a folder on worker1.** `movies` needs the 1.2 GB
  `data/embeddings/` file only to seed itself. `6-deploy.yml` copies it to
  `/srv/g2-data` on worker1, and `movies` mounts it read-only (`hostPath`) and is
  pinned to worker1. This keeps the images small (`images.md`: only build output
  goes into an image).
- **Secrets are not in git.** `6-deploy.yml` makes the JWT key pair, the three
  database passwords and the internal key once, in `infra/secrets/app/`
  (git-ignored), and turns them into the Secrets `jwt` and `g2`. `users` mounts both
  keys, `edge` only the public one.
- **Only `web` is reachable from outside.** Every other Service is ClusterIP. Ollama
  in particular has no authentication.

## Startup

On the first deploy `movies` downloads MovieLens and loads 87,585 vectors before it
reports started, which takes several minutes; its startup probe allows 20. Until
then search and recommendations are not ready. Later starts skip the seeding, since
`movies-db` keeps its data.

```sh
kubectl -n g2 get pods -w
```
