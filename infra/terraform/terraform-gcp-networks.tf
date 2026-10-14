# Elements of the cloud such as virtual servers,
# networks, firewall rules are created as resources
# syntax is: resource RESOURCE_TYPE RESOURCE_NAME
# https://developer.hashicorp.com/terraform/language/resources/syntax

# GCP's default network already has two rules we rely on:
#  - default-allow-internal: all TCP, UDP and ICMP between VMs in 10.128.0.0/9
#  - default-allow-ssh: port 22 from anywhere, on every VM (the build VM too)
# So SSH and ordinary node-to-node traffic need no rule here, and the build VM
# gets no other rule.

# What the outside world may reach on the cluster nodes.
resource "google_compute_firewall" "cluster_public" {
  name    = "cluster-public"
  network = "default"

  allow {
    protocol = "tcp"
    ports = [
      "80", "443",   # web traffic into the cluster
      "6443",        # Kubernetes API (kubectl from the mgmt container)
      "30000-32767", # the NodePort range Kubernetes hands out
    ]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["cluster"]
}

# What the nodes need from each other beyond default-allow-internal.
# ref: https://docs.tigera.io/calico/latest/getting-started/kubernetes/requirements
resource "google_compute_firewall" "cluster_internal" {
  name    = "cluster-internal"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["179"] # Calico BGP between nodes
  }

  # default-allow-internal covers only TCP, UDP and ICMP, so IP-in-IP
  # (IP protocol 4, Calico's encapsulation) must be allowed explicitly.
  allow {
    protocol = "ipip"
  }

  # Nodes talk over internal IPs, so these stay closed to the internet.
  source_ranges = ["10.128.0.0/9"]
  target_tags   = ["cluster"]
}
