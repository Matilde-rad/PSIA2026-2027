up: build
    docker compose up --build

build:
    cd app && mvn -B package -DskipTests
    cd frontend/web && npm ci && npm run build

down:
    docker compose down

reset:
    docker compose down -v
