// Demo users created through the edge public API, like real sign-ups. Needs Node 18+.
//
//   node scripts/seed-demo.mjs [baseUrl]   (default http://127.0.0.1:8000; dev mode: :8080)
//
// Every account has the password demo1234: local stacks only. Safe to re-run.

const PASSWORD = 'demo1234'

const GENRES = [
  'Action', 'Adventure', 'Animation', 'Children', 'Comedy', 'Crime', 'Documentary', 'Drama', 'Fantasy',
  'Film-Noir', 'Horror', 'IMAX', 'Musical', 'Mystery', 'Romance', 'Sci-Fi', 'Thriller', 'War', 'Western',
]

// Movies are a title search plus the release year, never catalog ids. A watchlist entry
// is [query, year, status] with status to_watch, liked or disliked.
const USERS = [
  {
    username: 'noir_nora', displayName: 'Nora Vance',
    bio: 'Rainy streets, fast talkers and a body in the first reel. I watch noir on repeat.',
    genres: ['Film-Noir', 'Crime', 'Mystery'], directors: ['Billy Wilder', 'Roman Polanski'],
    favorites: [['double indemnity', 1944], ['maltese falcon', 1941], ['chinatown', 1974]],
    watchlist: [['sunset', 1950, 'liked'], ['touch of evil', 1958, 'to_watch'], ['confidential', 1997, 'liked'],
      ['blade runner', 1982, 'to_watch'], ['fargo', 1996, 'liked'], ['casablanca', 1942, 'disliked']],
    follows: ['gangster_gus', 'indie_ines', 'sci_sam', 'romcom_rita'],
  },
  {
    username: 'sci_sam', displayName: 'Sam Okafor',
    bio: 'Hard sci-fi with big ideas about time, machines and first contact.',
    genres: ['Sci-Fi', 'Adventure', 'Thriller'], directors: ['Denis Villeneuve', 'Ridley Scott'],
    favorites: [['arrival', 2016], ['blade runner', 1982], ['interstellar', 2014], ['moon', 2009]],
    watchlist: [['matrix', 1999, 'liked'], ['2001 space odyssey', 1968, 'liked'], ['ex machina', 2014, 'to_watch'],
      ['alien', 1979, 'liked'], ['terminator 2', 1991, 'to_watch']],
    follows: ['noir_nora', 'action_alex', 'horror_hana', 'fantasy_finn'],
  },
  {
    username: 'toon_tess', displayName: 'Tess Lindgren',
    bio: 'Animation for every age, mostly Ghibli and Pixar. Always up for a rewatch.',
    genres: ['Animation', 'Children', 'Fantasy'], directors: ['Hayao Miyazaki'],
    favorites: [['spirited away', 2001], ['my neighbor totoro', 1988], ['toy story', 1995]],
    watchlist: [['princess mononoke', 1997, 'liked'], ['finding nemo', 2003, 'liked'], ['inside out', 2015, 'to_watch'],
      ['lion king', 1994, 'liked'], ['wall', 2008, 'to_watch'], ['neverending story', 1984, 'to_watch']],
    follows: ['fantasy_finn', 'musical_mia', 'romcom_rita'],
  },
  {
    username: 'horror_hana', displayName: 'Hana Takeda',
    bio: 'Slow dread over jump scares. Give me a haunted house and a long winter.',
    genres: ['Horror', 'Thriller', 'Mystery'], directors: ['Stanley Kubrick', 'John Carpenter'],
    favorites: [['shining', 1980], ['psycho', 1960], ['exorcist', 1973]],
    watchlist: [['halloween', 1978, 'liked'], ['get out', 2017, 'liked'], ['rosemary', 1968, 'to_watch'],
      ['thing', 1982, 'liked'], ['alien', 1979, 'liked'], ['grease', 1978, 'disliked']],
    follows: ['sci_sam', 'noir_nora', 'war_walt', 'gangster_gus'],
  },
  {
    username: 'romcom_rita', displayName: 'Rita Moreau',
    bio: 'Sweet, funny love stories and a good soundtrack. Comfort movies only.',
    genres: ['Romance', 'Comedy'], directors: ['Nora Ephron', 'Richard Curtis'],
    favorites: [['when harry met sally', 1989], ['notting hill', 1999], ['sleepless in seattle', 1993]],
    watchlist: [['pretty woman', 1990, 'liked'], ['amelie', 2001, 'liked'], ['clueless', 1995, 'liked'],
      ['annie hall', 1977, 'to_watch'], ['casablanca', 1942, 'to_watch'], ['mamma mia', 2008, 'liked']],
    follows: ['musical_mia', 'indie_ines', 'toon_tess', 'noir_nora'],
  },
  {
    username: 'war_walt', displayName: 'Walt Brennan',
    bio: 'War films and westerns, the older and dustier the better.',
    genres: ['War', 'Western', 'Drama'], directors: ['Sergio Leone', 'Clint Eastwood'],
    favorites: [['good bad ugly', 1966], ['unforgiven', 1992], ['saving private ryan', 1998]],
    watchlist: [['apocalypse now', 1979, 'liked'], ['full metal jacket', 1987, 'liked'], ['platoon', 1986, 'to_watch'],
      ['high noon', 1952, 'to_watch'], ['once upon a time in the west', 1968, 'liked']],
    follows: ['action_alex', 'gangster_gus', 'horror_hana'],
  },
  {
    username: 'docu_dan', displayName: 'Dan Reyes',
    bio: 'I like real stories told well: nature, politics, strange people.',
    genres: ['Documentary', 'Drama'], directors: ['Michael Moore', 'Werner Herzog'],
    favorites: [['bowling for columbine', 2002], ['grizzly man', 2005], ['man on wire', 2008]],
    watchlist: [['march of the penguins', 2005, 'liked'], ['super size me', 2004, 'liked'],
      ['inconvenient truth', 2006, 'to_watch'], ['fahrenheit', 2004, 'to_watch'], ['senna', 2010, 'liked']],
    follows: ['indie_ines', 'war_walt', 'sci_sam'],
  },
  {
    username: 'action_alex', displayName: 'Alex Kowalski',
    bio: 'Big stunts, clear geography and a one-liner at the right moment.',
    genres: ['Action', 'Adventure', 'Thriller'], directors: ['George Miller', 'James Cameron'],
    favorites: [['die hard', 1988], ['mad max fury road', 2015], ['dark knight', 2008]],
    watchlist: [['john wick', 2014, 'liked'], ['terminator 2', 1991, 'liked'], ['raiders of the lost ark', 1981, 'liked'],
      ['speed', 1994, 'to_watch'], ['gladiator', 2000, 'to_watch'], ['annie hall', 1977, 'disliked']],
    follows: ['sci_sam', 'war_walt', 'gangster_gus', 'fantasy_finn'],
  },
  {
    username: 'musical_mia', displayName: 'Mia Castellano',
    bio: 'If nobody is singing by minute ten, I get restless.',
    genres: ['Musical', 'Romance', 'Comedy'], directors: ['Damien Chazelle'],
    favorites: [['la la land', 2016], ['singin', 1952], ['sound of music', 1965]],
    watchlist: [['west side story', 1961, 'liked'], ['chicago', 2002, 'liked'], ['grease', 1978, 'liked'],
      ['moulin rouge', 2001, 'liked'], ['cabaret', 1972, 'to_watch'], ['mamma mia', 2008, 'to_watch']],
    follows: ['romcom_rita', 'toon_tess', 'indie_ines'],
  },
  {
    username: 'gangster_gus', displayName: 'Gus Moretti',
    bio: 'Crime families, loyalty and betrayal. Scorsese and Coppola are enough for a lifetime.',
    genres: ['Crime', 'Drama', 'Thriller'], directors: ['Martin Scorsese', 'Francis Ford Coppola'],
    favorites: [['godfather', 1972], ['goodfellas', 1990], ['scarface', 1983]],
    watchlist: [['casino', 1995, 'liked'], ['pulp fiction', 1994, 'liked'], ['reservoir dogs', 1992, 'liked'],
      ['departed', 2006, 'liked'], ['heat', 1995, 'to_watch'], ['mamma mia', 2008, 'disliked']],
    follows: ['noir_nora', 'action_alex', 'war_walt', 'horror_hana', 'indie_ines'],
  },
  {
    username: 'fantasy_finn', displayName: 'Finn Aberdeen',
    bio: 'Quests, strange creatures and maps at the front of the book.',
    genres: ['Fantasy', 'Adventure', 'Children'], directors: ['Peter Jackson', 'Guillermo del Toro'],
    favorites: [['fellowship of the ring', 2001], ['princess bride', 1987], ['labyrinth', 2006]],
    watchlist: [['harry potter sorcerer', 2001, 'liked'], ['princess mononoke', 1997, 'liked'], ['willow', 1988, 'to_watch'],
      ['big fish', 2003, 'to_watch'], ['stardust', 2007, 'liked'], ['neverending story', 1984, 'liked']],
    follows: ['toon_tess', 'sci_sam', 'action_alex'],
  },
  {
    username: 'indie_ines', displayName: 'Ines Duarte',
    bio: 'Small budgets, dry humor and awkward silences. Sundance if it were a person.',
    genres: ['Comedy', 'Drama', 'Romance'], directors: ['Wes Anderson', 'Sofia Coppola'],
    favorites: [['little miss sunshine', 2006], ['grand budapest hotel', 2014], ['rushmore', 1998]],
    watchlist: [['juno', 2007, 'liked'], ['napoleon dynamite', 2004, 'liked'], ['lost in translation', 2003, 'liked'],
      ['garden state', 2004, 'to_watch'], ['superbad', 2007, 'to_watch'], ['fargo', 1996, 'liked']],
    follows: ['romcom_rita', 'docu_dan', 'musical_mia', 'noir_nora'],
  },
]

const base = (process.argv[2] ?? 'http://127.0.0.1:8000').replace(/\/+$/, '')

function validate() {
  const problems = []
  const names = new Set(USERS.map((u) => u.username))
  for (const u of USERS) {
    if (!/^[a-z0-9_-]{3,32}$/.test(u.username)) problems.push(`${u.username}: bad username`)
    for (const g of u.genres) if (!GENRES.includes(g)) problems.push(`${u.username}: unknown genre ${g}`)
    if (u.favorites.length > 10) problems.push(`${u.username}: more than 10 favorite movies`)
    if (u.directors.length > 10) problems.push(`${u.username}: more than 10 favorite directors`)
    for (const f of u.follows) {
      if (!names.has(f)) problems.push(`${u.username}: follows unknown user ${f}`)
      if (f === u.username) problems.push(`${u.username}: follows themselves`)
    }
    for (const [, , status] of u.watchlist) {
      if (!['to_watch', 'liked', 'disliked'].includes(status)) problems.push(`${u.username}: bad status ${status}`)
    }
  }
  if (problems.length > 0) {
    console.error(problems.join('\n'))
    process.exit(1)
  }
}

class HttpError extends Error {
  constructor(method, path, status, body) {
    super(`${method} ${path} -> ${status} ${body}`)
    this.status = status
  }
}

async function call(method, path, { token, body } = {}) {
  const headers = { Accept: 'application/json' }
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  let res
  try {
    res = await fetch(base + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  } catch {
    console.error(`cannot reach ${base}: is the stack up? (just up)`)
    process.exit(1)
  }
  const text = await res.text()
  if (!res.ok) throw new HttpError(method, path, res.status, text)
  return text ? JSON.parse(text) : null
}

async function signIn(u) {
  try {
    await call('POST', '/auth/register', { body: { username: u.username, password: PASSWORD } })
  } catch (err) {
    if (err.status !== 409) throw err
    console.log(`${u.username}: exists, logging in`)
  }
  const login = await call('POST', '/auth/login', { body: { username: u.username, password: PASSWORD } })
  return { token: login.token, id: login.user.id }
}

// query + year -> catalog id, or null when the catalog has no such movie
const resolved = new Map()
async function resolve(token, [query, year]) {
  const key = `${query}|${year}`
  if (!resolved.has(key)) {
    const found = await call('GET', `/screens/movies/search?q=${encodeURIComponent(query)}`, { token })
    const hit = found.find((m) => m.year === year)
    if (!hit) console.warn(`skipping "${query}" (${year}): not in the catalog`)
    resolved.set(key, hit ? String(hit.movieId) : null)
  }
  return resolved.get(key)
}

async function main() {
  validate()
  const sessions = new Map()
  for (const u of USERS) sessions.set(u.username, await signIn(u))

  const anyToken = sessions.get(USERS[0].username).token
  for (const u of USERS) {
    for (const movie of [...u.favorites, ...u.watchlist]) await resolve(anyToken, movie)
  }
  if (![...resolved.values()].some(Boolean)) {
    console.error('no demo movie found: the catalog is not seeded yet (movies loads MovieLens on its first start)')
    process.exit(1)
  }

  // one user at a time: every profile save makes recommendations ask the AI
  for (const u of USERS) {
    const { token } = sessions.get(u.username)
    const favoriteMovies = (await Promise.all(u.favorites.map((m) => resolve(token, m)))).filter(Boolean)
    await call('PUT', '/me/profile', {
      token,
      body: {
        displayName: u.displayName,
        bio: u.bio,
        favoriteGenres: u.genres,
        favoriteDirectors: u.directors,
        favoriteMovies,
      },
    })
    for (const name of u.follows) await call('PUT', `/me/following/${sessions.get(name).id}`, { token })
    let watched = 0
    for (const [query, year, status] of u.watchlist) {
      const movieId = await resolve(token, [query, year])
      if (!movieId) continue
      await call('PUT', `/me/watchlist/${encodeURIComponent(movieId)}`, { token, body: { status } })
      watched++
    }
    console.log(`${u.username}: ${u.follows.length} follows, ${watched} watchlist entries`)
  }
  console.log(`${USERS.length} demo users ready, password ${PASSWORD}`)
}

main().catch((err) => {
  console.error(err.message)
  process.exit(1)
})
