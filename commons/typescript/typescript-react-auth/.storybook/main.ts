import { commonsSourceExports, serveTimeInjection, storybookManagerHead, storybookPreviewHead } from "@neckar-hub/vite-config";
import type { StorybookConfig } from "@storybook/react-vite";
import tailwindcss from "@tailwindcss/vite";
import { mergeConfig } from "vite";

const config: StorybookConfig = {
  framework: "@storybook/react-vite",
  stories: ["../src/**/*.stories.tsx"],
  core: { disableTelemetry: true },
  // No story names a `component`, so the argTypes react-docgen reads from it have no reader.
  typescript: { reactDocgen: false },
  managerHead: storybookManagerHead,
  previewHead: storybookPreviewHead,
  viteFinal: (viteConfig) => mergeConfig(viteConfig, { plugins: [tailwindcss(), commonsSourceExports(), serveTimeInjection()] }),
};

export default config;
