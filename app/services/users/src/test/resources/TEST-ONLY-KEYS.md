# TEST ONLY

`test-private.pem` and `test-public.pem` are a public, committed RSA key pair. The
`@QuarkusTest` classes use them through the `%test` properties in `application.properties`
to sign and verify JWTs.

- They protect nothing: the private key is in git, so anyone can forge a token with it.
- Never use them for dev or prod, and never copy them to `src/main`.
- Dev keys come from `app/keys/gen-dev-keys.sh`; prod keys come from a mounted secret.

Regenerate them with the same two commands as `app/keys/gen-dev-keys.sh`, writing the files
here:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out test-private.pem
openssl rsa -pubout -in test-private.pem -out test-public.pem
```
