import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { commonsSourceExports } from "./commons-source-exports.js";

/**
 * The two hooks under test, as the plain functions the plugin literal declares them to be. Vite
 * types both as an `ObjectHook`, which a call site would first have to narrow — the tests call the
 * functions directly instead.
 */
type ConfigHook = (config: Record<string, never>, env: { command: "serve"; mode: string }) => void;
type ResolveIdHook = (
  this: { resolve: () => Promise<{ id: string }> },
  id: string,
  importer: string | undefined,
  options: Record<string, never>,
) => Promise<string | undefined>;

/**
 * A commons package on disk, laid out the way the commons tsconfigs emit: `dist/src/index.js` as
 * the compiled output, and whichever source files the test wants beside it under `src/`.
 */
function commonsPackage(...sourceFileNames: string[]): { compiledFile: string; sourceDirectory: string } {
  const packageRoot = mkdtempSync(join(tmpdir(), "commons-source-exports-"));
  const sourceDirectory = join(packageRoot, "src");
  mkdirSync(join(packageRoot, "dist", "src"), { recursive: true });
  mkdirSync(sourceDirectory, { recursive: true });

  const compiledFile = join(packageRoot, "dist", "src", "index.js");
  writeFileSync(compiledFile, "");
  for (const sourceFileName of sourceFileNames) {
    writeFileSync(join(sourceDirectory, sourceFileName), "");
  }

  return { compiledFile, sourceDirectory };
}

/**
 * Drives the plugin directly: `config` puts it into development mode, and `resolveId` runs against
 * a stub context whose own resolver answers with the compiled file Vite's default resolution would
 * have picked.
 */
function pluginInDevelopmentMode(): (compiledFile: string, id?: string) => Promise<string | undefined> {
  const plugin = commonsSourceExports();
  (plugin.config as unknown as ConfigHook)({}, { command: "serve", mode: "development" });

  let resolvesTo = "";
  const context = { resolve: () => Promise.resolve({ id: resolvesTo }) };

  return (compiledFile: string, id = "@neckar-hub/typescript-utils") => {
    resolvesTo = compiledFile;
    return (plugin.resolveId as unknown as ResolveIdHook).call(context, id, "/app/src/main.tsx", {});
  };
}

describe("commonsSourceExports", () => {
  it("redirects a compiled import to its TypeScript source", async () => {
    const { compiledFile, sourceDirectory } = commonsPackage("index.ts");
    const resolve = pluginInDevelopmentMode();

    expect(await resolve(compiledFile)).toBe(join(sourceDirectory, "index.ts"));
  });

  it("falls back to the .tsx source when the .ts variant is absent", async () => {
    const { compiledFile, sourceDirectory } = commonsPackage("index.tsx");
    const resolve = pluginInDevelopmentMode();

    expect(await resolve(compiledFile)).toBe(join(sourceDirectory, "index.tsx"));
  });

  it("keeps the compiled output when the package ships no source beside it", async () => {
    const { compiledFile } = commonsPackage();
    const resolve = pluginInDevelopmentMode();

    // Undefined hands the import back to Vite's own resolution, which serves dist/. The layout that
    // makes this branch unreachable for a well-formed package is guarded repo-wide, not here.
    expect(await resolve(compiledFile)).toBeUndefined();
  });
});
