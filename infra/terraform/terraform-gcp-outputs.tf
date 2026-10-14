# To output values, follow pattern:
# value = TYPE.NAME.ATTR
# https://developer.hashicorp.com/terraform/language/values/outputs

# Each output is a map from VM name to IP, one per future Ansible group
# (master, workers, build), so the Ansible chunk can turn
# `terraform output -json` into an inventory.
# Public IPs are ephemeral, see terraform-gcp-servers.tf.

output "master_ips" {
  value = {
    (google_compute_instance.master.name) = google_compute_instance.master.network_interface[0].access_config[0].nat_ip
  }
}

# a set of identical instances created with "count"
output "worker_ips" {
  value = {
    for w in google_compute_instance.worker : w.name => w.network_interface[0].access_config[0].nat_ip
  }
}

output "build_ips" {
  value = {
    (google_compute_instance.build.name) = google_compute_instance.build.network_interface[0].access_config[0].nat_ip
  }
}

# The addresses Kubernetes nodes use to talk to each other. They stay the same
# across stop and start.
output "internal_ips" {
  value = merge(
    { (google_compute_instance.master.name) = google_compute_instance.master.network_interface[0].network_ip },
    { for w in google_compute_instance.worker : w.name => w.network_interface[0].network_ip },
    { (google_compute_instance.build.name) = google_compute_instance.build.network_interface[0].network_ip },
  )
}
