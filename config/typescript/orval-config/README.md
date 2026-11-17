# Generating Typescript Types and API Functions with Orval

Orval is a tool that generates Typescript types and functions to access REST API endpoints from an OpenAPI specification.

## Basic Usage

First Install the Dependencies in your project:

```shell
gradle installPnpmDevDependency -Pdependency=orval
gradle installPnpmDevDependency -Pdependency=@orval/core
gradle :kotlinUpgradePackageLock :pnpmInstall
```

Then add a configuration file `orval.config.ts` in the root of your project:

```typescript
import { defineConfig } from "orval";

const config: ConfigExternal = defineConfig({
  "<NAME_FOR_THE_CONFIGURATION>": {
    input: {
      target: "../path/to/your/openapi.json",
    },
    output: {
      target: "./build/generated/open-api/<FILE_NAME>",
      mode: "split",
  },
});

export default config;
```

Finally you need to create a script that runs Orval in the package.template.json:

```json
{
  "scripts": {
    "openapi:convert": "orval --config orval.config.ts"
  }
}
```

## Add-Ons

### Custom http Client

The custom http client will extend axios with interceptors for handling authentication and also convert ISO date strings
 to Date objects. You will need to configure the http client in your project.

```typescript
import { defineConfig } from "orval";

const config: ConfigExternal = defineConfig({
  "<NAME_FOR_THE_CONFIGURATION>": {
    input: {
      target: "../path/to/your/openapi.json",
    },
    output: {
      target: "./build/generated/open-api/<FILE_NAME>",
      mode: "split",
      override: {
        useDates: true,
        mutator: {
          path: "./path/to/the/httpClient.ts",
          name: "httpClient",
        },
      },
    },
  },
});

export default config;
```

### Add Comments about the Kotlin Source Code for the endpoints

The following configuration will add comments to the generated typescript code that points to the Kotlin sources of the 
endpoints.

```typescript
/**
 * Imports
 */

import { customTransformer } from "@neckar-hub/orval-config";
import { type ConfigExternal } from "@orval/core";
import { defineConfig } from "orval";

/**
 * Orval Configuration
 */

const config: ConfigExternal = defineConfig({
  "<NAME_FOR_THE_CONFIGURATION>": {
    input: {
      target: "../path/to/your/openapi.json",
    },
    output: {
      target: "./build/generated/open-api/<FILE_NAME>",
      mode: "split",
      override: {
        useDates: true,
        mutator: {
          path: "./path/to/the/httpClient.ts",
          name: "httpClient",
        },
        transformer: customTransformer,
      },
    },
  },
});

export default config;
```

### Extract example data from the OpenAPI specification

You can extract example data from the OpenAPI specification and use it in your Typescript code. This will create an 
examples folder with a file for each example type (`x-type-ref`) at the same place where the typescript file was generated.
Every Orval run deletes the examples folder first, so a file placed there by hand is lost.

```typescript
/**
 * Imports
 */

import { afterAllFilesWrite, customTransformer, extractExamples } from "@neckar-hub/orval-config";
import { type ConfigExternal } from "@orval/core";
import { defineConfig } from "orval";

/**
 * Orval Configuration
 */

const config: ConfigExternal = defineConfig({
  "<NAME_FOR_THE_CONFIGURATION>": {
    input: {
      target: "../path/to/your/openapi.json",
    },
    output: {
      target: "./build/generated/open-api/<FILE_NAME>",
      mode: "split",
      override: {
        useDates: true,
        mutator: {
          path: "./path/to/the/httpClient.ts",
          name: "httpClient",
        },
        transformer: customTransformer,
      },
      mock: extractExamples,
    },
    hooks: {
      afterAllFilesWrite,
    },
  },
});

export default config;
```

### URL functions for binary responses

`client: urlFunctionsClient` writes the `axios-functions` client plus `<operationName>Url` beside every GET operation with a binary response. For `GET /api/book-orders/{bookOrderId}/quote-pdf` it writes:

```typescript
export const getQuotePdfForBookOrderUrl = (bookOrderId: string): string => `/api/book-orders/${bookOrderId}/quote-pdf`;
```

```typescript
import { urlFunctionsClient } from "@neckar-hub/orval-config";

output: {
  target: "./build/generated/open-api/<FILE_NAME>",
  mode: "split",
  client: urlFunctionsClient,
},
```

A binary GET operation with query parameters stops the generation with an error naming the operation.
