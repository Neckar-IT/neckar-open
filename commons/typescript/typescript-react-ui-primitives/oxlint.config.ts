import sharedConfig from "@neckar-hub/oxlint-config";
import { defineConfig } from "oxlint";

// Files under `src/components/` stay unmodified shadcn CLI output, so the rules they violate are switched off per file.
export default defineConfig({
  ...sharedConfig,
  overrides: [
    {
      files: ["src/components/chart.tsx"],
      rules: {
        "react/jsx-no-constructed-context-values": "off",
        "typescript/no-deprecated": "off",
        "typescript/restrict-template-expressions": "off",
        "typescript/strict-boolean-expressions": "off",
      },
    },
    {
      files: ["src/components/input-group.tsx"],
      rules: {
        "jsx-a11y/no-noninteractive-element-interactions": "off",
      },
    },
    {
      files: ["src/components/progress.tsx"],
      rules: {
        "typescript/prefer-nullish-coalescing": "off",
        "typescript/strict-boolean-expressions": "off",
      },
    },
    {
      files: ["src/components/sidebar.tsx"],
      rules: {
        "typescript/no-confusing-void-expression": "off",
        "typescript/strict-boolean-expressions": "off",
      },
    },
    {
      files: ["src/components/toggle-group.tsx"],
      rules: {
        "react/jsx-no-constructed-context-values": "off",
      },
    },
  ],
});
