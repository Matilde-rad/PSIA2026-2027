#!/usr/bin/env bash
# DEV ONLY: generates an RSA-2048 key pair for `mvn quarkus:dev` in the users service.
#
# The keys are written to app/keys/dev/ (gitignored), outside every module's resources,
# so they can never end up in a jar. Never copy them into src/main/resources.
# Production keys come from a mounted secret (see the README, "Auth keys (users)").
#
# Usage: ./app/keys/gen-dev-keys.sh [--force]
#   Without --force, existing keys are left untouched.
set -euo pipefail

dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/dev"
mkdir -p "$dir"

if [[ -e "$dir/privateKey.pem" && "${1:-}" != "--force" ]]; then
    echo "keys already exist in $dir (use --force)"
    exit 0
fi

# the private key must not be readable by other local users
umask 077

# genpkey always writes PKCS#8 ("BEGIN PRIVATE KEY"), which smallrye-jwt requires;
# the legacy genrsa may write "BEGIN RSA PRIVATE KEY", which it rejects
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$dir/privateKey.pem"
openssl rsa -pubout -in "$dir/privateKey.pem" -out "$dir/publicKey.pem"
chmod 644 "$dir/publicKey.pem"

echo "dev keys written to $dir"
