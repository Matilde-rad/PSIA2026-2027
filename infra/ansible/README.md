# infra/ansible

Installs Kubernetes on the VMs created by `../terraform`, with kubeadm, turns the
`build` VM into the image builder and registry, and deploys the app. The cluster steps follow lab4 (node
prep, install, cluster init, workers join), written so they are safe to re-run.

The result is Kubernetes 1.37 with containerd 2.x and the Calico v3.33.0 network
(pod CIDR 192.168.0.0/16) on `master` and `worker1` .. `worker3`, plus a container
registry on the `build` VM at `<build internal IP>:5000` holding the `users`, `movies`,
`recommendations`, `ai`, `edge` and `web` images, and the app running in the `g2`
namespace at `http://<worker1 public IP>`.

## Files

- `ansible.cfg`: inventory, SSH user and key, no host key checking.
- `inventory.py`: turns `terraform output -json` into `inventory.ini` (generated, git-ignored).
- `1-node-prep.yml`: kernel modules, sysctls, swap off.
- `2-k8s-install.yml`: containerd, then kubelet, kubeadm and kubectl from pkgs.k8s.io.
- `3-cluster-init.yml`: `kubeadm init` on the master, Calico, the kubeconfig.
- `4-workers-join.yml`: joins the workers.
- `5-build-images.yml`: on the build VM, the registry, the build and the image pushes.
- `6-deploy.yml`: the app's Secrets and manifests (`../../deploy`) on the cluster.
- `7-monitoring.yml`: Prometheus and Grafana.
- `8-ci-runner.yml`: a GitLab Runner on the build VM, for `.gitlab-ci.yml`.
- `9-argocd.yml`: Argo CD, which deploys `deploy/` from the repo.
- `site.yml`: runs the steps in order.
- `../secrets/kubeconfig`: cluster-admin access for `kubectl`, written by step 3. Git-ignored.
- `../secrets/app/`: the app's JWT keys, database passwords and internal key, made by step 6. Git-ignored.

## Before you start

The VMs exist (see `../terraform/README.md`) and the management container is running.
If it was started before `git`, `rsync` and the `ansible.posix` collection were added
to the Dockerfile (step 5 needs them), restart it so the image is rebuilt:

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
ansible-playbook site.yml  # the six steps in one go
kubectl get nodes
```

Or step by step, in this order:

```sh
ansible-playbook 1-node-prep.yml
ansible-playbook 2-k8s-install.yml
ansible-playbook 3-cluster-init.yml
ansible-playbook 4-workers-join.yml
ansible-playbook 5-build-images.yml   # or `just images` on the host
ansible-playbook 6-deploy.yml         # or `just deploy` on the host
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
5. **Build images** (build VM). It installs Docker, JDK 21, `just`, Node 22 (from
   NodeSource, as Vite needs 20.19 or later) and Maven 3.9.12 (from Apache, checked
   against its sha512, as Ubuntu has 3.8.7). It starts a `registry:2.8.3` container on
   port 5000 that restarts with Docker and keeps its data in `/var/lib/registry`. It
   rsyncs the repo to `/home/ubuntu/g2`, without `.git/`, `target/`, `node_modules/`,
   `infra/secrets/` and `data/` (not part of the build), and runs `just build` there, the same build as on our
   machines. Then it builds the five service images from `app/Dockerfile` (one per
   `MODULE`) and the web image from `frontend/web/Dockerfile` (see
   `../../deploy/images.md`), tags each with the git short SHA and `latest`, and pushes
   them to `<build internal IP>:5000`.

The registry has no TLS and no login. That is fine here because only VMs inside the
VPC can reach port 5000 (GCP's `default-allow-internal` rule); no rule opens it to the
internet. Two settings make the plain HTTP work: Docker on the build VM lists it under
`insecure-registries`, and step 1 gives containerd on every node a
`/etc/containerd/certs.d/<registry>/hosts.toml` that says `http://`. Step 2 sets
containerd's `config_path` so that file is read. In Kubernetes manifests the images are
`<build internal IP>:5000/users:<sha>` and so on.

The SHA is read in the mgmt container from the checked-out commit, so commit before
building, or the `<sha>` tag will hold uncommitted changes.

6. **Deploy** (worker1, then the mgmt container). It copies `data/embeddings/` (1.2 GB,
   only the first time) to `/srv/g2-data` on worker1, where `movies` reads it. Then,
   with `kubectl`: it installs local-path-provisioner v0.0.37, which gives the cluster
   the `local-path` StorageClass for the database and Ollama volumes. It makes the
   JWT key pair, the database passwords and the internal key once in
   `../secrets/app/`, and stores them as the Secrets `jwt` and `g2` in the `g2`
   namespace. Then it copies `../../deploy` to a temporary directory, writes the
   registry and the current git short SHA into the copy's `kustomization.yaml`, and
   runs `kubectl apply -k` on it, so the tracked file never changes. The manifests
   and the choices behind them are in `../../deploy/README.md`.

Step 6 deploys the images of the current commit, so run step 5 first after each new
commit (or deploy another build with `-e tag=<sha>`).

After step 6, `kubectl -n g2 get pods -w` shows the pods starting. The first time,
`movies` takes several minutes to seed its database. When everything is `Ready`, the
app is at `http://<worker1 public IP>` (the playbook prints it).

Compared with lab4:

- Built-in Ansible modules instead of big shell blocks, so a re-run changes nothing.
- No cluster wipe before `kubeadm init`: init and join skip themselves when done.
- No CPU preflight override: e2-standard-2 has the 2 vCPUs kubeadm wants.
- The join command never touches a file (lab4 used a world-writable file in `/tmp`).
- The inventory is generated from Terraform, not edited by hand.
- Images are built on our own build VM and kept in our own registry, instead of a
  public one.

## CI/CD (steps 8 and 9)

Step 8 installs a GitLab Runner on the build VM and step 9 installs Argo CD in the
cluster. Together with `../../.gitlab-ci.yml` they test, package and deploy every
commit; the pipeline itself is described under *CI/CD* in the root README.

Both need a token from GitLab first, saved in `../secrets/` (git-ignored):

1. **Runner token.** In the GitLab project: *Settings > CI/CD > Runners > New project
   runner*, tag `g2-build`, leave *Run untagged jobs* unchecked, then *Create runner*.
   Copy the `glrt-...` token into `../secrets/gitlab-runner-token`.
2. **Deploy token** (Argo CD reads the repo). *Settings > Repository > Deploy tokens >
   Add token*, scope `read_repository`. Save it as one line `username:token` in
   `../secrets/gitlab-deploy-token`.
3. **Push token** (the pipeline's `deploy` job commits the new tag). *Settings > Access
   tokens > Add new token*, role `Maintainer`, scope `write_repository`. Add it under
   *Settings > CI/CD > Variables* as `GITOPS_TOKEN`, masked and protected (`deploy`
   only runs on `master`, which GitLab protects by default). If project access
   tokens are unavailable, a personal access token with `write_repository` works the
   same way.

```sh
ansible-playbook 8-ci-runner.yml   # or `just runner` on the host
ansible-playbook 9-argocd.yml      # or `just argocd` on the host
```

- **Runner.** gitlab-runner v18.4.0 with the shell executor, as the `gitlab-runner`
  user in the `docker` group, so jobs use the Docker, JDK, Maven and Node from step 5
  and the Maven cache in its home survives between jobs. Registering sets
  `REGISTRY=<build internal IP>:5000` for every job. The registry needs no login,
  since only the VPC reaches it. Re-running the playbook does not re-register; to
  register again, delete `/etc/gitlab-runner/config.toml` on the build VM first.
- **Argo CD** v3.1.0 in the `argocd` namespace, UI over plain HTTP at
  `http://<worker1 public IP>:30880`, user `admin`, password saved to
  `../secrets/argocd-admin-password`. The Application `g2` follows `deploy/` on
  `master`, with automatic sync, pruning and self-heal.
- Argo CD only manages what is in `deploy/kustomization.yaml`. The Secrets, the node
  label and local-path-provisioner still come from step 6, so step 6 runs once before
  step 9. After step 9, deploy by pushing to `master`: a `just deploy` would be undone
  by self-heal at the next sync.
- `deploy/kustomization.yaml` holds `latest` until the first pipeline on `master`, so
  Argo CD starts from the images step 5 pushed.

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

## Checking the registry

From any VM (`ssh`, or `ansible build -a ...`):

```sh
curl http://<build internal IP>:5000/v2/_catalog          # the six images
curl http://<build internal IP>:5000/v2/users/tags/list   # <sha> and latest
```

## Re-running and starting over

Every playbook can be re-run; re-running step 5 builds and pushes the current commit.
The registry and its images survive a stop and start of the build VM. After `terraform destroy` and a new `terraform apply`
the VMs are new, so run all the steps again, starting with `python3 inventory.py`.

## Notes

- Ansible prints "Found both group and host with same name" for `master` and
  `build`. It is harmless: the group names match the VM names.
- The first rsync in step 6 sends the 1.2 GB embeddings to worker1. Later runs only
  send changes, and step 5 does not copy `data/` at all.
- Postgres reads its password only when it first creates its database. Deleting
  `../secrets/app/` after that would leave the databases with the old passwords.
- `../secrets/kubeconfig` is cluster-admin access. Never commit or share it.
- GCP's `ubuntu` user has passwordless sudo, so no sudoers changes are needed.
