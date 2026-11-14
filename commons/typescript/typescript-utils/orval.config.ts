/**
 * Imports
 */

import { customTransformer, inputTransformer, jsDocFilter } from "@neckar-hub/orval-config";
import type { ConfigExternal } from "@orval/core";
import { defineConfig } from "orval";

/**
 * Orval Configuration
 */

const config: ConfigExternal = defineConfig({
  "openapi-types": {
    input: {
      target: "./build/generated/open-api/merged-openapi-source-files.json",
      override: {
        transformer: inputTransformer,
      },
    },
    output: {
      target: "./build/generated/open-api/openapi-types.ts",
      mode: "split",
      override: {
        transformer: customTransformer,
        jsDoc: {
          filter: jsDocFilter,
        },
      },
    },
  },
});

export default config;
