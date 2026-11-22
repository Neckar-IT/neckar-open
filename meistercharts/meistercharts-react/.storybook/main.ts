import { storybookManagerHead, storybookPreviewHead } from "@neckar-hub/vite-config";
import type { StorybookConfig } from "@storybook/react-vite";

const config: StorybookConfig = {
  framework: { name: "@storybook/react-vite", options: { builder: { viteConfigPath: ".storybook/vite.config.ts" } } },
  stories: ["../src/**/*.stories.tsx"],
  addons: ["@storybook/addon-docs"],
  core: { disableTelemetry: true },
  typescript: {
    reactDocgen: "react-docgen-typescript",
    // The dev server also loads the commons sources, which lie outside this package's TypeScript project.
    reactDocgenTypescriptOptions: { include: ["src/**/*.tsx"] },
  },
  managerHead: storybookManagerHead,
  previewHead: storybookPreviewHead,
};

export default config;
