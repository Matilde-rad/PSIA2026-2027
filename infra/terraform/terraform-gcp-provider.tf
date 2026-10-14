# Terraform Google Cloud deployment for g2: the VMs of the Kubernetes cluster
# and one build VM.

# The Terraform and provider versions are pinned. `terraform init` writes
# .terraform.lock.hcl, which records the exact provider build; commit it.
# https://developer.hashicorp.com/terraform/language/providers/requirements
terraform {
  required_version = "~> 1.16"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.7"
    }
  }
}

# How to configure the provider:
# https://registry.terraform.io/providers/hashicorp/google/latest/docs
# The key file lives in the git-ignored infra/secrets/ and is never committed.
provider "google" {
  credentials = file(var.GCP_CREDENTIALS_FILE)
  project     = var.GCP_PROJECT_ID
  zone        = var.GCP_ZONE
}
