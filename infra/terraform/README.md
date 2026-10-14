# infra/terraform

Provisions the VMs the Kubernetes cluster runs on. Self-hosted only the spec
forbids managed Kubernetes (GKE, EKS, AKS).

Terraform's job stops at machines, network and firewall rules. Installing
Kubernetes onto those machines is `../ansible`.

## What it creates

| Name | Machine type | Role | Disk |
| --- | --- | --- | --- |
| `master` | e2-standard-2 | Kubernetes control plane | 30 GB |
| `worker1` .. `worker3` | e2-standard-2 | Kubernetes workers | 30 GB |
| `build` | e2-standard-4 | builds and pushes images, not in the cluster | 50 GB |

All VMs run Ubuntu 24.04 LTS, get an ephemeral public IP and accept the SSH key
made by `just keys` for the user `ubuntu`.

Two firewall rules apply to the VMs tagged `cluster` (master and workers):
`cluster-public` opens 80, 443, 6443 (Kubernetes API) and the NodePort range
30000-32767 to the internet, and `cluster-internal` allows Calico's BGP (179) and
IP-in-IP only from the internal range 10.128.0.0/9. SSH and the rest of the
node-to-node TCP/UDP traffic come from the default network's own rules
(`default-allow-ssh`, `default-allow-internal`), which is also all the build VM
gets.

## Files

- `terraform-gcp-provider.tf`: pinned Terraform and Google provider versions, and the provider settings.
- `terraform-gcp-variables.tf`: project ID, key paths, zone, image, machine types, worker count.
- `terraform-gcp-networks.tf`: the two firewall rules.
- `terraform-gcp-servers.tf`: master, workers (with `count`) and the build VM.
- `terraform-gcp-outputs.tf`: name-to-IP maps for each group.
- `../Dockerfile`: the management image (Terraform, Ansible, gcloud, kubectl), pinned versions.
- `../secrets/`: the GCP key and the SSH keys. Git-ignored, never committed.

## One-time setup

1. Prerequisites: Docker (Docker Engine on Linux, Docker Desktop on macOS and
   Windows), `just`, and `ssh-keygen` (preinstalled on Linux and macOS). Nothing
   else is installed on the host: Terraform, Ansible, gcloud and kubectl come
   from the management container. (On Windows, run the commands inside WSL with
   Docker Desktop's WSL integration on.)
2. Create a GCP project with billing (credits) and note its project ID. Enable
   the Compute Engine API (APIs and Services, Enable APIs, "Compute Engine API").
   Enabling it also creates the `default` network these files use.
3. Create the service-account key: IAM and Admin, Service Accounts, the Compute
   Engine default service account, Manage keys, Add key, JSON. Run
   `mkdir -p infra/secrets` and save the downloaded file as
   `infra/secrets/gcp-key.json`. It is git-ignored; never commit it or put it
   anywhere else in the repo.
4. Run `just keys` to create `infra/secrets/id_ed25519` and `id_ed25519.pub`.
   Terraform installs the public key on every VM for the user `ubuntu`. An
   existing key is never overwritten.
5. Set the project ID by writing `GCP_PROJECT_ID = "your-project-id"` to
   `infra/terraform/terraform.tfvars` (git-ignored), or with
   `export TF_VAR_GCP_PROJECT_ID=your-project-id` inside the container. The other
   variables (zone, machine types, worker count) can be overridden the same way.

## Create the VMs

```sh
just mgmt-start     # builds the image the first time, then starts the container
just mgmt           # shell inside it, the repo is at /g2
```

Inside the container:

```sh
cd infra/terraform
terraform init      # commit the .terraform.lock.hcl it creates
terraform plan
terraform apply
terraform output
```

The four outputs are maps from VM name to IP: `master_ips`, `worker_ips` and
`build_ips` hold the public IPs (one map per future Ansible group), `internal_ips`
holds the internal IPs of all VMs. The Ansible chunk turns `terraform output -json`
into its inventory.

Optional check with gcloud:

```sh
gcloud auth activate-service-account --key-file=/g2/infra/secrets/gcp-key.json
gcloud config set project your-project-id
gcloud compute instances list
```

## IP addresses

Public IPs are ephemeral and change when a VM is stopped and started. The
cluster keeps working because the nodes talk over internal IPs, which GCP keeps.
To get the new public IPs run `terraform apply -refresh-only` and then
`terraform output`.

## Clean up

Run `terraform destroy` when you are done, then check in the console that
nothing is left. Running VMs spend credits, and so do the disks of stopped VMs,
a little. Then run `just mgmt-stop` (the container is removed). The state stays in
`infra/terraform/terraform.tfstate`, which is git-ignored and must not be lost
while VMs exist.

## Notes

- The container runs as uid 1000 so the state files stay owned by you. On a Linux
  host with a different uid, change `USER` in `../Dockerfile`; Docker Desktop on
  macOS and Windows handles this automatically.
- If GCP says a zone lacks resources, set `GCP_ZONE` to another zone.
