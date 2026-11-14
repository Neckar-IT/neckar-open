import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { Plugin } from "vite";

/** Name the emitted asset is hashed from, and the name the dev server answers under. */
const ScriptFileName = "theme-init.js";
const DevScriptName = `@neckar-${ScriptFileName}`;

/** The script this plugin ships, beside the plugin. */
const ScriptPath = join("src", "theme-init", ScriptFileName);

/**
 * Applies the persisted theme before the first paint, from a same-origin classic script: an inline
 * block is what `script-src 'self'` parses and never runs, a module is what the browser defers past
 * the paint. `vite build` emits it as an asset, `vite dev` serves it under [DevScriptName].
 */
export function themeInit(): Plugin {
  const scriptSource: string = readFileSync(join(packageRoot(), ScriptPath), "utf8");

  let buildMode = false;
  let base = "/";
  let assetReferenceId: string | undefined;
  let scriptUrl = `/${DevScriptName}`;

  return {
    name: "neckar-theme-init",

    configResolved(config) {
      buildMode = config.command === "build";
      base = config.base;
      // Overwritten with the emitted asset in generateBundle; what stays is the dev server's answer.
      scriptUrl = `${base}${DevScriptName}`;
    },

    buildStart() {
      // emitFile has no bundle to write to in serve.
      if (!buildMode) return;

      assetReferenceId = this.emitFile({ type: "asset", name: ScriptFileName, source: scriptSource });
    },

    generateBundle() {
      // The hashed name is settled here, and vite:build-html transforms the HTML after this hook —
      // a core build plugin, so it runs behind every normal plugin of the config.
      if (assetReferenceId === undefined) {
        // Skipping would ship the dev path to production, where it 404s without an error anywhere.
        throw new Error("themeInit(): buildStart emitted no asset, so the tag has no file to point at");
      }

      scriptUrl = `${base}${this.getFileName(assetReferenceId)}`;
    },

    configureServer(server) {
      server.middlewares.use((request, response, next) => {
        // Matched on the name, because whether the base is still on the URL here depends on where
        // this middleware ends up in the dev server's stack.
        if (request.url?.split("?")[0]?.endsWith(`/${DevScriptName}`) !== true) {
          next();
          return;
        }

        // This middleware writes the response itself, so nothing else declares its encoding.
        response.setHeader("Content-Type", "text/javascript; charset=utf-8");
        response.end(scriptSource);
      });
    },

    // Appended to the head, so the charset stays the first tag and the script still runs before the
    // module entry in the body.
    transformIndexHtml() {
      return [{ tag: "script", attrs: { src: scriptUrl }, injectTo: "head" }];
    },
  };
}

/**
 * Walked rather than computed from a fixed depth: this module is imported from `dist/src/theme-init/` and
 * typechecked from `src/theme-init/`, which sit at different distances from the script it has to read.
 */
function packageRoot(): string {
  let directory = dirname(fileURLToPath(import.meta.url));

  while (!existsSync(join(directory, "package.json"))) {
    const parent = dirname(directory);
    if (parent === directory) {
      throw new Error(`themeInit(): no package.json above ${fileURLToPath(import.meta.url)}`);
    }
    directory = parent;
  }

  return directory;
}
