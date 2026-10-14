# Management container: Terraform, Ansible, gcloud and kubectl in one place, the same
# idea as lab4's mgmt, so nothing infra-related is installed on the host.
# It is built from stdin with no build context (`docker build - < infra/Dockerfile`),
# so nothing from the repo, and in particular nothing from infra/secrets, can end up
# in the image. The repo is mounted at /g2 at run time instead.

# Ubuntu 24.04 (noble) dated snapshot. An exact tag, so rebuilds give the same result
# (the pinned-versions rule in deploy/images.md).
FROM ubuntu:noble-20260917

# Terraform release to install.
ARG TERRAFORM_VERSION=1.16.5
# Keep kubectl within one minor version of the cluster's Kubernetes version, which is
# chosen in the Ansible chunk.
ARG KUBECTL_VERSION=v1.37.1
# gcloud release from Google's apt repo.
ARG GCLOUD_VERSION=589.0.0

# Prevent interactive prompts during package installation.
ENV DEBIAN_FRONTEND=noninteractive

# --no-install-recommends keeps the image small. ansible-core is the ansible commands
# plus the built-in modules, from Ubuntu 24.04's own archive; extra collections can be
# added with ansible-galaxy if a later playbook needs them. openssh-client is how
# Ansible reaches the VMs.
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl gnupg unzip openssh-client ansible-core && \
    rm -rf /var/lib/apt/lists/*

# Terraform is a single binary, so the release zip is lighter than adding HashiCorp's
# apt repo as lab4 did.
RUN curl -fsSL -o /tmp/terraform.zip "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_$(dpkg --print-architecture).zip" && \
    unzip /tmp/terraform.zip terraform -d /usr/local/bin && \
    rm /tmp/terraform.zip

# kubectl: single binary from the Kubernetes release site.
RUN curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/$(dpkg --print-architecture)/kubectl" && \
    chmod 0755 /usr/local/bin/kubectl

# gcloud from Google's apt repo, pinned to one release. Used for checks such as
# listing instances; Terraform itself only needs the key file.
RUN curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg | gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" > /etc/apt/sources.list.d/google-cloud-sdk.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends google-cloud-cli=${GCLOUD_VERSION}-0 && \
    rm -rf /var/lib/apt/lists/*

# The image's built-in user has uid 1000, the usual uid of the first user on Linux.
# Docker Desktop on macOS and Windows maps file ownership on mounts to the host user
# anyway. Files written to the mounted repo (terraform.tfstate, .terraform/) therefore
# stay owned by the developer rather than root, which is what happened in lab4. On a
# Linux host whose uid is not 1000, change this. ssh also needs a real user, which is
# why the image doesn't pass an arbitrary --user at run time.
USER ubuntu

# The repo is mounted here.
WORKDIR /g2

# Keeps the container alive so `just mgmt` can exec into it. Lab4 ran sshd under
# supervisord, which is not needed here because we only use docker exec.
CMD ["sleep", "infinity"]
