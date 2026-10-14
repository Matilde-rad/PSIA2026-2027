# infra/ansible

Installs Kubernetes on the VMs created by `../terraform`, with kubeadm. The steps
follow lab4 (node prep, install, cluster init, workers join), written so they are
safe to re-run.

The result is Kubernetes 1.37 with containerd 2.x and the Calico v3.33.0 network
(pod CIDR 192.168.0.0/16) on `master` and `worker1` .. `worker3`. The `build` VM is
in the inventory, but no playbook targets it yet.

## Files

- `ansible.cfg`: inventory, SSH user and key, no host key checking.
- `inventory.py`: turns `terraform output -json` into `inventory.ini` (generated, git-ignored).
- `1-node-prep.yml`: kernel modules, sysctls, swap off.
- `2-k8s-install.yml`: containerd, then kubelet, kubeadm and kubectl from pkgs.k8s.io.
- `3-cluster-init.yml`: `kubeadm init` on the master, Calico, the kubeconfig.
- `4-workers-join.yml`: joins the workers.
- `site.yml`: runs the four steps in order.
- `../secrets/kubeconfig`: cluster-admin access for `kubectl`, written by step 3. Git-ignored.

## Before you start

The VMs exist (see `../terraform/README.md`) and the management container is running.
If it was started before `KUBECONFIG` and `ANSIBLE_CONFIG` were added to the Dockerfile,
restart it so the image is rebuilt:

```sh
just mgmt-stop
just mgmt-start
just mgmt
```

Everything below runs inside the container. The host only needs `just`.

## Run

```sh
cd infra/ansible
python3 inventory.py       # writes inventory.ini from the Terraform outputs
ansible all -m ping        # every VM answers over SSH
ansible-playbook site.yml  # the four steps in one go
kubectl get nodes
```

Or step by step, in this order:

```sh
ansible-playbook 1-node-prep.yml
ansible-playbook 2-k8s-install.yml
ansible-playbook 3-cluster-init.yml
ansible-playbook 4-workers-join.yml
```

`kubectl get nodes` should show something like this. The nodes turn `Ready` after a
minute or two, once Calico's pods have started.

```text
NAME      STATUS   ROLES           AGE   VERSION
master    Ready    control-plane   3m    v1.37.1
worker1   Ready    <none>          2m    v1.37.1
worker2   Ready    <none>          2m    v1.37.1
worker3   Ready    <none>          2m    v1.37.1
```

## What each step does

1. **Node prep** (all nodes). It loads the `overlay` and `br_netfilter` kernel modules
   now and on every boot, and sets the sysctls that let traffic cross the Linux bridge
   and be forwarded between pods. It turns swap off, because the kubelet refuses to
   start with swap (GCP images have none, so this is normally a no-op). It first waits
   for cloud-init, which holds the apt lock on a fresh VM.
2. **Install** (all nodes). It installs containerd from Ubuntu 24.04, which is 2.x (Kubernetes
   1.38 and later need that), and sets `SystemdCgroup = true` so containerd and the
   kubelet use the same cgroup driver. It adds the signed pkgs.k8s.io v1.37 apt repo,
   installs kubelet, kubeadm and kubectl at exactly 1.37.1-1.1, and holds them so an
   apt upgrade cannot change the cluster's version.
3. **Cluster init** (master). `kubeadm init` advertises the master's internal IP, because
   the nodes talk over internal IPs, and adds the public IP as an extra name in the API
   server certificate, so `kubectl` in the container can reach it. Then it applies the
   Calico manifest, pinned to v3.33.0. Finally it copies `admin.conf` to
   `../secrets/kubeconfig` (mode 0600) with the server address changed to
   `https://<master public IP>:6443`. The container finds it through `KUBECONFIG`.
4. **Workers join** (workers). The master makes a join command with a token that
   lives for one hour, and each worker runs it. The command only exists in Ansible's
   memory; it is not written to a file or printed.

Compared with lab4:

- Built-in Ansible modules instead of big shell blocks, so a re-run changes nothing.
- No cluster wipe before `kubeadm init`: init and join skip themselves when done.
- No CPU preflight override: e2-standard-2 has the 2 vCPUs kubeadm wants.
- The join command never touches a file (lab4 used a world-writable file in `/tmp`).
- The inventory is generated from Terraform, not edited by hand.

## When the master's public IP changes

Public IPs change when a VM is stopped and started. The cluster itself is not
affected, since the nodes use internal IPs. Only your `kubectl` needs the new address:

```sh
cd /g2/infra/terraform && terraform apply -refresh-only
cd /g2/infra/ansible
python3 inventory.py
ansible-playbook 3-cluster-init.yml --tags kubeconfig
```

The kubeconfig checks the API server by the name `kubernetes` (`tls-server-name`),
which its certificate always has. So only the server address changes, and no
certificate is re-issued.

## Re-running and starting over

Every playbook can be re-run. After `terraform destroy` and a new `terraform apply`
the VMs are new, so run all the steps again, starting with `python3 inventory.py`.

## Notes

- Ansible prints "Found both group and host with same name" for `master` and
  `build`. It is harmless: the group names match the VM names.
- `../secrets/kubeconfig` is cluster-admin access. Never commit or share it.
- GCP's `ubuntu` user has passwordless sudo, so no sudoers changes are needed.
