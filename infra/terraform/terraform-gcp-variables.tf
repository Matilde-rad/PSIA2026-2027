# How to define variables in terraform:
# https://developer.hashicorp.com/terraform/language/values/variables

# ID of the project (not its display name), shown in the GCP console when
# clicking on the project name (on the top dropdown).
# It has no default on purpose: set it in a git-ignored terraform.tfvars with
# the line GCP_PROJECT_ID = "..." or with export TF_VAR_GCP_PROJECT_ID=...
variable "GCP_PROJECT_ID" {
  type = string
}

# Service-account JSON key downloaded from the console (see README), relative
# to infra/terraform.
variable "GCP_CREDENTIALS_FILE" {
  type    = string
  default = "../secrets/gcp-key.json"
}

# Public SSH key installed on every VM, created by `just keys`.
variable "SSH_PUBLIC_KEY_FILE" {
  type    = string
  default = "../secrets/id_ed25519.pub"
}

# Regions list is found at:
# https://cloud.google.com/compute/docs/regions-zones
# If GCP reports that the zone lacks resources, try another zone.
variable "GCP_ZONE" {
  type    = string
  default = "europe-west4-b"
}

# Ubuntu 24.04 LTS image family. A family resolves to its newest image when the
# VM is created. Other images can be found at:
# https://cloud.google.com/compute/docs/images/os-details
variable "GCP_IMAGE" {
  type    = string
  default = "ubuntu-os-cloud/ubuntu-2404-lts-amd64"
}

# Machine type of the master and the workers: 2 vCPU / 8 GB. kubeadm requires at
# least 2 CPUs on the control plane, so no preflight-error override is needed.
# A list of machine types is found at:
# https://cloud.google.com/compute/docs/machine-resource
# Prices are defined per region, check the cost at:
# https://cloud.google.com/compute/vm-instance-pricing
variable "GCP_MACHINE_TYPE" {
  type    = string
  default = "e2-standard-2"
}

# Number of worker VMs (worker1 ... workerN).
variable "GCP_WORKER_COUNT" {
  type    = number
  default = 3
}

# Machine type of the build VM: 4 vCPU / 16 GB, for the Maven, npm and docker
# builds and image pushes of a later chunk.
variable "GCP_BUILD_MACHINE_TYPE" {
  type    = string
  default = "e2-standard-4"
}
