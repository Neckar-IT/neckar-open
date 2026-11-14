import { commonsSourceExports, serveTimeInjection } from "@neckar-hub/vite-config";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";

/**
 * Vite Configuration exclusively for ladle stories.
 * This is necessary to include the tailwindcss plugin, which is required for the tailwind classes to work in the stories.
 */
export default defineConfig({
  plugins: [tailwindcss(), commonsSourceExports(), serveTimeInjection()],
});
