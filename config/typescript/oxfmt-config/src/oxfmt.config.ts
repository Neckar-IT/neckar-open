import { defineConfig } from "oxfmt";

export default defineConfig({
  printWidth: 140,
  tabWidth: 2,
  semi: true,
  singleQuote: false,
  trailingComma: "all",
  arrowParens: "always",
  bracketSameLine: false,
  proseWrap: "preserve",
  sortImports: {
    newlinesBetween: false,
  },
  sortTailwindcss: {
    functions: ["cn", "cva"],
  },
  ignorePatterns: [
    "**/build/",
    "**/dist/",
    "**/node_modules/",
    "**/.gradle/",
    "**/.astro/",
    "**/*.generated.json",
    "pnpm-lock.yaml",
  ],
});
