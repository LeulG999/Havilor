import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server sends every /api request on to the Spring Boot backend.
// The browser only ever talks to one address (localhost:5173), so the backend needs no CORS settings.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
