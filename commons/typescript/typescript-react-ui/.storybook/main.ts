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
    // The primitives this package re-exports belong to `typescript-react-ui-primitives`; the docgen project includes them.
    reactDocgenTypescriptOptions: {
      tsconfigPath: ".storybook/tsconfig.docgen.json",
      include: ["src/**/*.tsx", "../typescript-react-ui-primitives/src/**/*.tsx"],
    },
  },
  managerHead: storybookManagerHead,
  previewHead: storybookPreviewHead,
  viteFinal: (viteConfig) =>
    mergeConfig(viteConfig, {
      plugins: [tailwindcss(), commonsSourceExports({ alsoInBuild: true }), serveTimeInjection(), mswServiceWorker()],
    }),
};

export default config;
