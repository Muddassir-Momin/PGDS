import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Port 3000 matches the backend CORS setting (CORS_ORIGIN, default http://localhost:3000)
export default defineConfig({
  plugins: [react()],
  server: { port: 3000, host: true },
});
