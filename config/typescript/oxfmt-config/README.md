# Oxfmt Config

This package provides the shared configuration for the Oxfmt code formatter. It is used by both the CLI and the VS Code extension.

## Usage

1. Add the package as a devDependency and the format scripts to your project's `package.template.json`:+
+
```json
{
  "devDependencies": {
    "@neckar-hub/oxfmt-config": "workspace:*"
  },
  "scripts": {
    "format": "oxfmt --check",
    "format:fix": "oxfmt --write"
  }
}
```

2. Create an `oxfmt.config.ts` file in the root of your project with the following content:+
+
```typescript
import { sharedconfig } from "@neckar-hub/oxfmt-config";

export default sharedConfig;
```

3. Add the `oxfmt.config.ts` file to the `tsconfig.json`:+
+
```json
"include": [ "./oxfmt.config.json" ]
```

## Reason for a separate package

The Oxfmt CLI and IDE extension would be able to walk up the directory tree and find the configuration file in the Monorepo Root.
We chose to create a separate package for the Oxfmt configuration because of two reasons:

1. It allows for easier customization of the configuration for a single project.
2. There is a quirk with the configuration discovery, where it tries to read a vite.config.ts file when it cannot find the oxfmt.config.ts file. See here: https://github.com/oxc-project/oxc/issues/20416