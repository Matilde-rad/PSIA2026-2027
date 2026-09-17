# deploy

Kubernetes manifests.

Empty for now. Directories to create as each piece lands:

    apps/<component>/   Deployment, Service, and the component's own Postgres
    monitoring/         Prometheus + Grafana, dashboard JSON
    argocd/             Argo CD Application manifests

## Open decision: Gateway API vs Ingress

Whichever it is, the route's backend port must match the `Service` port, which
is not necessarily the container's 8080.

## Manifests are hand-written

Each component needs a Deployment and a Service written by hand under
`apps/<component>/`.

Services listen on 8080 in the container. `users`, `movies` and
`recommendations` expose SmallRye Health, so their probes go to
`/q/health/live`, `/q/health/ready` and `/q/health/started`. `edge` has no
health extension yet, so it has no probe endpoints to point at.
