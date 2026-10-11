# TEST ONLY

`test-private.pem` and `test-public.pem` are the same public, committed RSA key pair as in
the users service. The `@QuarkusTest` classes use them: the `%test` property in
`application.properties` makes edge verify tokens with `test-public.pem`, and only the test
helper `Tokens` signs with `test-private.pem`.

- They protect nothing: the private key is in git, so anyone can forge a token with it.
- Never use them for dev or prod, and never copy them to `src/main`.
- Dev keys come from `app/keys/gen-dev-keys.sh`; prod keys come from a mounted secret.
