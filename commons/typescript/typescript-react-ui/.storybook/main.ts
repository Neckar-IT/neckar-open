import {
  commonsSourceExports,
  mswServiceWorker,
  serveTimeInjection,
  storybookManagerHead,
  storybookPreviewHead,
} from "@neckar-hub/vite-config";
import type { StorybookConfig } from "@storybook/react-vite";
import tailwindcss from "@tailwindcss/vite";
import { mergeConfig } from "vite";

const config: StorybookConfig = {
  framework: "@storybook/react-vite",
  stories: ["../src/**/*.stories.tsx"],
  // Hands every story the `msw` worker its `beforeEach` registers request handlers with.
  addons: ["msw-storybook-addon"],
  core: { disableTelemetry: true },
  // No story names a `component`, so the argTypes react-docgen reads from it have no reader.
  typescript: { reactDocgen: false },
  managerHead: storybookManagerHead,
  previewHead: storybookPreviewHead,
  viteFinal: (viteConfig) =>
    mergeConfig(viteConfig, { plugins: [tailwindcss(), commonsSourceExports(), serveTimeInjection(), mswServiceWorker()] }),
};

export default config;
