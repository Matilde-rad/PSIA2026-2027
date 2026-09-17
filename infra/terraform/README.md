# infra/terraform

Provisions the VMs the Kubernetes cluster runs on. Self-hosted only the spec
forbids managed Kubernetes (GKE, EKS, AKS).

Terraform's job stops at machines, network and firewall rules. Installing
Kubernetes onto those machines is `../ansible`.

Empty for now.
