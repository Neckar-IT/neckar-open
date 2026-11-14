import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { build } from "vite";
import { afterEach, describe, expect, it, vi } from "vitest";
import { resolveServeTimeValues, serveTimeInjection } from "./serve-time-injection.js";

/**
 * The hook under test, as the plain function the plugin literal declares it to be. Vite types
 * `transformIndexHtml` as an `ObjectHook`, which a call site would first have to narrow.
 */
type TransformIndexHtmlHook = { handler: (html: string) => string };

const ImageDigest = "sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221";

/** Every placeholder the plugin knows, taken from the values themselves so the test grows with them. */
const placeholderNames = Object.keys(resolveServeTimeValues());

/** A head carrying each placeholder under a meta tag named after it, as a participating frontend writes it. */
function headWithEveryPlaceholder(): string {
  const metaTags = placeholderNames.map((name) => `    <meta name="${name}" content="\${${name}}" />`).join("\n");
  return `<!doctype html>\n<html>\n  <head>\n${metaTags}\n  </head>\n  <body></body>\n</html>\n`;
}

function transformIndexHtml(html: string): string {
  return (serveTimeInjection().transformIndexHtml as TransformIndexHtmlHook).handler(html);
}

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("serveTimeInjection", () => {
  it("substitutes every placeholder the entrypoint substitutes", () => {
    vi.stubEnv("APP_IMAGE_DIGEST", ImageDigest);
    const transformed = transformIndexHtml(headWithEveryPlaceholder());

    expect(transformed).not.toContain("${");
  });

  it("leaves the image digest a placeholder without an image", () => {
    vi.stubEnv("APP_IMAGE_DIGEST", undefined);

    expect(transformIndexHtml(`<meta name="imageDigest" content="\${APP_IMAGE_DIGEST}" />`)).toBe(
      `<meta name="imageDigest" content="\${APP_IMAGE_DIGEST}" />`,
    );
  });

  it("leaves the image digest a placeholder for an unpinned deploy", () => {
    vi.stubEnv("APP_IMAGE_DIGEST", "unpinned");

    expect(resolveServeTimeValues().APP_IMAGE_DIGEST).toBeUndefined();
  });

  it("leaves the image digest a placeholder for a malformed value in the environment", () => {
    vi.stubEnv("APP_IMAGE_DIGEST", "sha256:not-hex");

    expect(resolveServeTimeValues().APP_IMAGE_DIGEST).toBeUndefined();
    expect(resolveServeTimeValues().APP_IMAGE_DIGEST_SHORT).toBeUndefined();
  });

  it("derives the short image digest from the digest the environment sets", () => {
    vi.stubEnv("APP_IMAGE_DIGEST", ImageDigest);

    expect(resolveServeTimeValues().APP_IMAGE_DIGEST_SHORT).toBe("ad2453b02334");
  });

  it("reports the runtime context the Gradle run tasks give the backend", () => {
    const values = resolveServeTimeValues();

    expect(values.EXECUTION_ENVIRONMENT).toBe("LocalDev");
    expect(values.DEPLOYMENT_STAGE).toBe("Development");
    expect(values.SERVICE_HOST).toBe("localhost");
  });

  it("lets the environment override a runtime context axis", () => {
    vi.stubEnv("DEPLOYMENT_STAGE", "Demo");

    expect(transformIndexHtml(`<meta content="\${DEPLOYMENT_STAGE}" />`)).toBe(`<meta content="Demo" />`);
  });

  it("inserts a value with a dollar sign literally, as envsubst does", () => {
    vi.stubEnv("SERVICE_HOST", "host$&name");

    expect(transformIndexHtml(`<meta content="\${SERVICE_HOST}" />`)).toBe(`<meta content="host$&name" />`);
  });

  it("leaves a placeholder it does not know", () => {
    const untouched = `<meta content="\${SOMETHING_ELSE}" />`;

    expect(transformIndexHtml(untouched)).toBe(untouched);
  });

  // The regression this guards is silent: a substituted value makes dist/index.html differ
  // per environment, which costs every frontend package its up-to-date state without failing anything.
  it("leaves every placeholder in the HTML vite build writes", async () => {
    const root = mkdtempSync(join(tmpdir(), "serve-time-injection-"));
    writeFileSync(join(root, "index.html"), headWithEveryPlaceholder());

    await build({
      root,
      configFile: false,
      logLevel: "silent",
      plugins: [serveTimeInjection()],
    });

    const built = readFileSync(join(root, "dist", "index.html"), "utf8");
    for (const name of placeholderNames) {
      expect(built).toContain(`\${${name}}`);
    }
  }, 60_000);
});
