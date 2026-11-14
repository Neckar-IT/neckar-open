import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// VITE_PORT sets the port of the dev server; without it vite serves on its default port.
const devPort = process.env.VITE_PORT ? Number(process.env.VITE_PORT) : undefined;

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Listen on all interfaces so Docker (via host.docker.internal) can reach the dev server
    host: "0.0.0.0",
    port: devPort,
    strictPort: true,
  },
});
