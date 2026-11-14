import { mkdirSync, mkdtempSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { Plugin } from "vite";
import { build } from "vite";
import { beforeAll, describe, expect, it } from "vitest";
import { themeInit } from "./theme-init-plugin.js";

/**
 * The serve-mode hooks under test, as the plain functions the plugin literal declares them to be.
 * Vite types them as `ObjectHook`, which a call site would first have to narrow.
 */
type ConfigResolvedHook = (config: { command: string; base: string }) => void;
type ConfigureServerHook = (server: { middlewares: { use: (middleware: DevMiddleware) => void } }) => void;
type TransformIndexHtmlHook = () => { attrs: { src: string } }[];
type DevMiddleware = (request: { url?: string }, response: DevResponse, next: () => void) => void;

interface DevResponse {
  setHeader: (name: string, value: string) => void;
  end: (body: string) => void;
}

interface DevRequestResult {
  headers: Record<string, string>;
  body?: string;
  passedOn: boolean;
}

/** A head without a theme block of its own, as a participating frontend writes it. */
const IndexHtml = `<!doctype html>\n<html>\n  <head>\n    <meta charset="UTF-8" />\n    <title>Test</title>\n  </head>\n  <body>\n    <div id="root"></div>\n    <script type="module" src="/src/main.tsx"></script>\n  </body>\n</html>\n`;

const TestDirectory = dirname(fileURLToPath(import.meta.url));

/** The file the plugin ships — the one the ThemeProvider's storage keys are shared with. */
const ShippedScript = readFileSync(join(TestDirectory, "theme-init.js"), "utf8");

const ThemeProviderPath = join(
  TestDirectory,
  "../../../../../commons/typescript/typescript-react-ui/src/providers/theme/ThemeProvider.tsx",
);

/** Read rather than imported: the keys are module constants of the provider, and this package depends on no commons package. */
const ProviderSource = readFileSync(ThemeProviderPath, "utf8");

function storageKeyOf(constantName: string): string {
  const key = new RegExp(`const ${constantName} = "([^"]+)"`).exec(ProviderSource)?.[1];
  if (key === undefined) {
    throw new Error(`ThemeProvider.tsx declares no ${constantName}, so the script has nothing to agree with`);
  }
  return key;
}

interface BuiltFrontend {
  html: string;
  scriptFileName: string;
  scriptSource: string;
}

function frontendRoot(prefix: string): string {
  const root = mkdtempSync(join(tmpdir(), prefix));
  writeFileSync(join(root, "index.html"), IndexHtml);
  mkdirSync(join(root, "src"));
  writeFileSync(join(root, "src", "main.tsx"), "export {};\n");
  return root;
}

async function buildFrontend(plugin: Plugin = themeInit(), base?: string): Promise<BuiltFrontend> {
  const root = frontendRoot("theme-init-");

  await build({ root, base, configFile: false, logLevel: "silent", plugins: [plugin] });

  const assetsDir = join(root, "dist", "assets");
  const scriptFileNames = readdirSync(assetsDir).filter((name) => name.startsWith("theme-init"));
  // Exactly one: a second would make every assertion below pick an arbitrary asset.
  expect(scriptFileNames).toHaveLength(1);
  const scriptFileName = scriptFileNames[0] ?? "";

  return {
    html: readFileSync(join(root, "dist", "index.html"), "utf8"),
    scriptFileName,
    scriptSource: readFileSync(join(assetsDir, scriptFileName), "utf8"),
  };
}

describe("themeInit", () => {
  let built: BuiltFrontend;

  beforeAll(async () => {
    built = await buildFrontend();
  }, 60_000);

  it("loads the script from the emitted asset and writes nothing inline", () => {
    expect(built.html).toContain(`<script src="/assets/${built.scriptFileName}"></script>`);
    expect(built.html).not.toMatch(/<script(?![^>]*\ssrc=)/);
  });

  // Vite hoists the module entry into the head ahead of this tag, so what runs the theme script
  // first is not its position but its being classic: a module would be deferred past the paint.
  it("puts a classic script in the head, behind the charset", () => {
    const emittedTag = new RegExp(`<script[^>]*${built.scriptFileName.replaceAll(".", "\\.")}[^>]*>`).exec(built.html)?.[0];

    expect(emittedTag).toBeDefined();
    expect(emittedTag).not.toContain("type=");

    const scriptIndex = built.html.indexOf(emittedTag ?? "");
    expect(scriptIndex).toBeGreaterThan(built.html.indexOf("<meta charset"));
    expect(scriptIndex).toBeLessThan(built.html.indexOf("</head>"));
  });

  // The tag has to name whatever the hash made of the file, so a fixed name in the tag is a 404.
  it("hashes the asset name", () => {
    expect(built.scriptFileName).toMatch(/^theme-init-[A-Za-z0-9_-]+\.js$/);
  });

  // What ships is the file beside the ThemeProvider, not a copy the plugin carries as a string.
  it("emits the script file of this package unchanged", () => {
    expect(built.scriptSource).toBe(ShippedScript);
  });

  // Taken from the provider's source, so renaming a key there fails here instead of leaving the
  // pre-paint script reading keys nobody writes.
  it("reads the storage keys the ThemeProvider writes", () => {
    expect(built.scriptSource).toContain(`"${storageKeyOf("ThemeStorageKey")}"`);
    expect(built.scriptSource).toContain(`"${storageKeyOf("ResolvedColorSchemeStorageKey")}"`);
  });

  it("prefixes the built asset URL with a configured base", async () => {
    const underBase = await buildFrontend(themeInit(), "/app/");

    expect(underBase.html).toContain(`<script src="/app/assets/${underBase.scriptFileName}"></script>`);
  }, 60_000);

  // The dev server has no bundle to emit into, so it reaches the script over its own path. Without
  // this the second of the plugin's two paths is only ever exercised by hand.
  it("serves the script from the dev server under a configured base", () => {
    const served = servePlugin("/app/");

    expect(served.scriptSrc).toBe("/app/@neckar-theme-init.js");

    const response = served.request(served.scriptSrc);

    expect(response.headers["Content-Type"]).toBe("text/javascript; charset=utf-8");
    expect(response.body).toBe(ShippedScript);
  });

  it("passes a request for another path on to the next middleware", () => {
    const response = servePlugin("/app/").request("/app/src/main.tsx");

    expect(response.passedOn).toBe(true);
    expect(response.body).toBeUndefined();
  });
});

/**
 * The plugin in serve mode: the tag it injects, and the middleware it registers on the dev server.
 * Driven through the hooks rather than through a listening server, which would need a free port.
 */
function servePlugin(base: string): { scriptSrc: string; request: (url: string) => DevRequestResult } {
  const plugin = themeInit();
  (plugin.configResolved as ConfigResolvedHook)({ command: "serve", base });

  let middleware: DevMiddleware | undefined;
  (plugin.configureServer as ConfigureServerHook)({
    middlewares: {
      use: (registered) => {
        middleware = registered;
      },
    },
  });

  return {
    // Double cast because Vite's ObjectHook union admits the `{ handler }` form the plugin does not use.
    scriptSrc: (plugin.transformIndexHtml as unknown as TransformIndexHtmlHook)()[0]?.attrs.src ?? "",

    request(url: string): DevRequestResult {
      const headers: Record<string, string> = {};
      const result: DevRequestResult = { headers, passedOn: false };

      middleware?.(
        { url },
        {
          setHeader: (name, value) => {
            headers[name] = value;
          },
          end: (written) => {
            result.body = written;
          },
        },
        () => {
          result.passedOn = true;
        },
      );

      return result;
    },
  };
}
