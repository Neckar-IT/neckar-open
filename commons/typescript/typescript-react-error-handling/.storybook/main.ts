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
  // `msw-storybook-addon` hands every story the `msw` worker its `beforeEach` registers request handlers with.
  addons: ["msw-storybook-addon", "@storybook/addon-docs"],
  core: { disableTelemetry: true },
  typescript: {
    reactDocgen: "react-docgen-typescript",
    // The dev server also loads the commons sources, which lie outside this package's TypeScript project.
    reactDocgenTypescriptOptions: { include: ["src/**/*.tsx"] },
  },
  managerHead: storybookManagerHead,
  previewHead: storybookPreviewHead,
  viteFinal: (viteConfig) =>
    mergeConfig(viteConfig, { plugins: [tailwindcss(), commonsSourceExports(), serveTimeInjection(), mswServiceWorker()] }),
};

export default config;
