import { existsSync } from "node:fs";
import type { Plugin } from "vite";

const WORKSPACE_SCOPE = "@neckar-hub/";

export interface CommonsSourceExportsOptions {
  /**
   * Redirects in a production build too. A Storybook build sets it, because react-docgen-typescript reads the
   * props table of a component only from its TypeScript source.
   */
  alsoInBuild?: boolean;
}

/**
 * Vite plugin that, in dev mode, redirects imports of `@neckar-hub/*`
 * commons packages from their compiled `dist/*.js` output to the matching
 * `src/*.ts` or `src/*.tsx` source file — enabling Vite HMR on commons edits
 * without a `tsc --watch` step.
 *
 * The plugin implements its own `resolveId` hook, so the dev-mode HMR wiring
 * needs no exports condition in the commons `package.json` files.
 *
 * In production (`mode !== "development"`) the hook short-circuits and Vite
 * falls back to its default resolution, which continues to pick the standard
 * `"import"` exports condition and read `dist/`.
 *
 * See issue #1454.
 */
export function commonsSourceExports(options: CommonsSourceExportsOptions = {}): Plugin {
  let redirect = false;

  return {
    name: "neckar-commons-source-exports",
    enforce: "pre",

    config(_config, env) {
      redirect = env.mode === "development" || options.alsoInBuild === true;
    },

    async resolveId(id, importer, resolveOptions) {
      if (redirect === false) return;
      if (id.startsWith(WORKSPACE_SCOPE) === false) return;

      // Delegate to Vite's default resolver so pnpm workspace links, the
      // standard "import" condition, and subpath exports all still apply.
      // skipSelf prevents an infinite loop back into this hook.
      const resolved = await this.resolve(id, importer, { ...resolveOptions, skipSelf: true });
      if (resolved === null || resolved === undefined) return;
      if (resolved.id.includes("/dist/") === false) return;

      // Rewrite dist/src/*.js → src/*.ts (or .tsx if the .ts variant is absent).
      // The commons tsconfigs emit to dist/ with src/ preserved as a subdirectory,
      // so the path mapping strips that intermediate "src/" segment.
      const asTs = resolved.id.replace("/dist/src/", "/src/").replace(/\.js$/, ".ts");
      if (existsSync(asTs)) return asTs;

      const asTsx = asTs.replace(/\.ts$/, ".tsx");
      if (existsSync(asTsx)) return asTsx;

      // No source beside the compiled output: keep it. The dist/src/ layout the mapping relies on is
      // enforced by verifyWorkspacePackageEmitLayout.
      return;
    },
  };
}
