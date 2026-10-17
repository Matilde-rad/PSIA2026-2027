up: build
    docker compose up --build

build:
    cd app && mvn -B package -DskipTests
    cd frontend/web && npm ci && npm run build

down:
    docker compose down

reset:
    docker compose down -v

# demo users through edge (password demo1234); url is the compose web port, or edge in dev mode
seed url="http://127.0.0.1:8000":
    node scripts/seed-demo.mjs {{url}}

# --- GCP infrastructure (see infra/terraform/README.md) ---

# SSH key pair Terraform puts on the VMs (user ubuntu); kept out of git in infra/secrets/
keys:
    mkdir -p infra/secrets
    test -f infra/secrets/id_ed25519 || ssh-keygen -t ed25519 -N "" -C g2-infra -f infra/secrets/id_ed25519

# build and start the management container (terraform, ansible, gcloud, kubectl); the repo is mounted at /g2
mgmt-start:
    docker build -t g2-mgmt - < infra/Dockerfile
    docker run --rm -d --name g2-mgmt --hostname mgmt -v "{{justfile_directory()}}:/g2" g2-mgmt

# shell inside the management container
mgmt:
    docker exec -it g2-mgmt bash

# stop the management container (it is removed on stop)
mgmt-stop:
    docker stop g2-mgmt

# build and push the images on the build VM
images:
    docker exec -w /g2/infra/ansible g2-mgmt ansible-playbook 5-build-images.yml

# deploy the app with the images of the current commit
deploy:
    docker exec -w /g2/infra/ansible g2-mgmt ansible-playbook 6-deploy.yml

# GitLab Runner on the build VM (needs infra/secrets/gitlab-runner-token)
runner:
    docker exec -w /g2/infra/ansible g2-mgmt ansible-playbook 8-ci-runner.yml

# Argo CD, which then deploys master (needs infra/secrets/gitlab-deploy-token)
argocd:
    docker exec -w /g2/infra/ansible g2-mgmt ansible-playbook 9-argocd.yml
