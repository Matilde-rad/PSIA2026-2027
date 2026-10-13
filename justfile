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
