import { localDevServerHost } from "@neckar-hub/vite-config";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// VITE_PORT sets the port of the dev server; without it vite serves on its default port.
const devPort = process.env.VITE_PORT ? Number(process.env.VITE_PORT) : undefined;

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: localDevServerHost(),
    port: devPort,
    strictPort: true,
  },
});
