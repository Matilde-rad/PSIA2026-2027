# Management container: Terraform, Ansible, gcloud, kubectl and helm.

FROM ubuntu:noble-20260917

ARG TERRAFORM_VERSION=1.16.5
ARG KUBECTL_VERSION=v1.37.1
ARG GCLOUD_VERSION=589.0.0
ARG ANSIBLE_POSIX_VERSION=2.2.2
ARG HELM_VERSION=v4.3.0

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl gnupg unzip openssh-client ansible-core git rsync && \
    rm -rf /var/lib/apt/lists/*

RUN ansible-galaxy collection install ansible.posix==${ANSIBLE_POSIX_VERSION} -p /usr/share/ansible/collections

RUN git config --system safe.directory /g2

RUN curl -fsSL -o /tmp/terraform.zip "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_$(dpkg --print-architecture).zip" && \
    unzip /tmp/terraform.zip terraform -d /usr/local/bin && \
    rm /tmp/terraform.zip

RUN curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/$(dpkg --print-architecture)/kubectl" && \
    chmod 0755 /usr/local/bin/kubectl

RUN arch=$(dpkg --print-architecture) && \
    curl -fsSL -o /tmp/helm.tar.gz "https://get.helm.sh/helm-${HELM_VERSION}-linux-${arch}.tar.gz" && \
    echo "$(curl -fsSL "https://get.helm.sh/helm-${HELM_VERSION}-linux-${arch}.tar.gz.sha256sum" | cut -d' ' -f1)  /tmp/helm.tar.gz" | sha256sum -c - && \
    tar -xzf /tmp/helm.tar.gz -C /usr/local/bin --strip-components=1 "linux-${arch}/helm" && \
    rm /tmp/helm.tar.gz

RUN curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg | gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" > /etc/apt/sources.list.d/google-cloud-sdk.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends google-cloud-cli=${GCLOUD_VERSION}-0 && \
    rm -rf /var/lib/apt/lists/*

USER ubuntu

WORKDIR /g2

ENV KUBECONFIG=/g2/infra/secrets/kubeconfig
ENV ANSIBLE_CONFIG=/g2/infra/ansible/ansible.cfg

CMD ["sleep", "infinity"]
