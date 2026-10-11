import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The browser talks to one origin (the Vite server) and Vite forwards the API paths to edge,
// so edge needs no CORS. 127.0.0.1 because localhost may resolve to another container over IPv6 (see the README).
const EDGE = 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/auth': EDGE,
      '/me': EDGE,
      '/screens': EDGE,
    },
  },
})
