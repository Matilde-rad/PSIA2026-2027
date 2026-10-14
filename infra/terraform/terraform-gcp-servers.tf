# Elements of the cloud such as virtual servers,
# networks, firewall rules are created as resources
# syntax is: resource RESOURCE_TYPE RESOURCE_NAME
# https://developer.hashicorp.com/terraform/language/resources/syntax

###########  Master   #############
resource "google_compute_instance" "master" {
  name         = "master"
  machine_type = var.GCP_MACHINE_TYPE
  zone         = var.GCP_ZONE

  boot_disk {
    initialize_params {
      # image list can be found at:
      # https://console.cloud.google.com/compute/images
      image = var.GCP_IMAGE
      # size is in GB. The 10 GB default is too small for the app's container
      # images (including Ollama).
      size = 30
    }
  }

  network_interface {
    network = "default"
    # An empty access_config gives the VM an ephemeral public IP (no static
    # reservation). kubeadm nodes talk over internal IPs, which GCP keeps across
    # stop and start. Only the public IPs change: after a restart run
    # `terraform apply -refresh-only` and then `terraform output` to get the new
    # ones for the inventory.
    access_config {}
  }

  # GCP creates the user ubuntu with this key; Ansible logs in as ubuntu.
  metadata = {
    ssh-keys = "ubuntu:${file(var.SSH_PUBLIC_KEY_FILE)}"
  }

  tags = ["cluster", "master"]
}

###########  Workers   #############
# This method creates as many identical instances as the "count" index value
resource "google_compute_instance" "worker" {
  count        = var.GCP_WORKER_COUNT
  name         = "worker${count.index + 1}"
  machine_type = var.GCP_MACHINE_TYPE
  zone         = var.GCP_ZONE

  # Same disk, network and key as the master, see above.
  boot_disk {
    initialize_params {
      image = var.GCP_IMAGE
      size  = 30
    }
  }

  network_interface {
    network = "default"
    access_config {}
  }

  metadata = {
    ssh-keys = "ubuntu:${file(var.SSH_PUBLIC_KEY_FILE)}"
  }

  tags = ["cluster", "worker"]
}

###########  Build VM   #############
# Builds the images and pushes them to the registry (a later chunk).
resource "google_compute_instance" "build" {
  name         = "build"
  machine_type = var.GCP_BUILD_MACHINE_TYPE
  zone         = var.GCP_ZONE

  boot_disk {
    initialize_params {
      image = var.GCP_IMAGE
      # Room for the Maven repository, npm, docker images and the 1.2 GB
      # embeddings file.
      size = 50
    }
  }

  network_interface {
    network = "default"
    access_config {}
  }

  metadata = {
    ssh-keys = "ubuntu:${file(var.SSH_PUBLIC_KEY_FILE)}"
  }

  # Not part of the cluster: without the cluster tag no cluster firewall rule
  # applies, so the VM is reachable only over SSH through default-allow-ssh.
  tags = ["build"]
}
