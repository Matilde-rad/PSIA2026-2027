# frontend/web — Cinemate web UI

React 19 + Vite 8, plain JavaScript and CSS. It talks only to `edge`.

Deliberately **not** a Maven module and absent from `app/pom.xml`'s `<modules>`:
the Java build and the frontend build stay independent.

## Run it (dev)

Needs Node 20.19+ or 22.12+ and npm.

```sh
./app/keys/gen-dev-keys.sh                       # once, from the repo root
cd app/services/users           && mvn quarkus:dev   # :8081
cd app/services/recommendations && mvn quarkus:dev   # :8083
cd app/edge                     && mvn quarkus:dev   # :8080
```

Then, in another terminal:

```sh
cd frontend/web
npm install
npm run dev
```

Open http://127.0.0.1:5173.

Home's For you tab and your own profile need the recommendations service running:
edge calls it for them and answers 502 without it. Home's Search tab also needs `ai`,
`movies` and Ollama (see the root README, *AI and recommendations slice: run locally*);
without them a search shows "The AI is unavailable right now".

## How it reaches edge

Vite's dev server forwards `/auth`, `/me` and `/screens` to http://127.0.0.1:8080
(`vite.config.js`). The browser sees one origin, so edge needs no CORS.
The screens edge serves are `/screens/home` (Home, For you tab), `/screens/search`
(Home, Search tab) and `/screens/profile/ID`.

`npm run build` writes `dist/`. Serving it in production belongs to the deploy work.

## Real vs preview

| Real | Static preview ("Preview · coming soon") |
|------|-------------------------------------------|
| Register (you are logged in right after; username and password checked as you type) | Profile: bio, favorite genres, Edit profile form (Matilde's Profile service) |
| Login and logout | Social: name search and genre matches (Matilde's Social Discovery service) |
| Redirects for logged-out and logged-in users | |
| Profile header (username, member since) for any user id at `/profile/ID` | |
| Home: For you, your recommendations as a banner carousel and grid (`/screens/home`) | |
| Home: Search, Ask AI returns 12 movies with posters (`/screens/search`) | |

## Session

The token and the user are kept in localStorage under `g2.session`. Tokens last 1 h.
Any 401 sends you to the login page with "Your session expired, please log in again."
Log out only clears the browser.

## Code map

- `src/main.jsx` — starts React.
- `src/App.jsx` — routes, guards, nav.
- `src/api.js` — the only place that calls edge.
- `src/auth.jsx` — session state and login, register, logout.
- `src/pages/` — one file per screen, plus `ForYou.jsx` (Home's For you tab).
- `src/components/` — small shared pieces (Avatar, MovieCard, PreviewBadge).
- `src/previews/` — one file per preview, replaced when its backend exists.
- `src/styles.css` — all styles; design tokens at the top.

There are no frontend tests yet (D-66); the phase was checked by a manual walkthrough.
