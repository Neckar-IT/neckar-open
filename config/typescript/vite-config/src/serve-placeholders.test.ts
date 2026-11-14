import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { build } from "vite";
import { afterEach, describe, expect, it } from "vitest";
import { servePlaceholders, substitutePlaceholders } from "./serve-placeholders.js";

type TransformIndexHtmlHook = { handler: (html: string, ctx?: { originalUrl?: string }) => string };

function transformIndexHtml(plugin: ReturnType<typeof servePlaceholders>, html: string, originalUrl?: string): string {
  return (plugin.transformIndexHtml as TransformIndexHtmlHook).handler(html, originalUrl === undefined ? undefined : { originalUrl });
}

const temporaryRoots: string[] = [];

afterEach(() => {
  temporaryRoots.splice(0).forEach((root) => rmSync(root, { recursive: true, force: true }));
});

describe("servePlaceholders", () => {
  it("replaces every occurrence of a name", () => {
    const plugin = servePlaceholders({ bookDataJson: '{"title":"Kotlin in Action"}' });

    expect(transformIndexHtml(plugin, "<a>${bookDataJson}</a><b>${bookDataJson}</b>")).toBe('<a>{"title":"Kotlin in Action"}</a><b>{"title":"Kotlin in Action"}</b>');
  });

  it("leaves a name without a value and a name it does not know", () => {
    const html = "${ABSENT} ${UNKNOWN}";

    expect(transformIndexHtml(servePlaceholders({ ABSENT: undefined }), html)).toBe(html);
  });

  it("inserts a value with a dollar sign literally", () => {
    expect(substitutePlaceholders("${NAME}", { NAME: "a$&b" })).toBe("a$&b");
  });

  it("evaluates a function source at each request", () => {
    let current = "first";
    const plugin = servePlaceholders(() => ({ NAME: current }));

    expect(transformIndexHtml(plugin, "${NAME}")).toBe("first");
    current = "second";
    expect(transformIndexHtml(plugin, "${NAME}")).toBe("second");
  });

  it("passes the request URL to a function source", () => {
    const plugin = servePlaceholders(({ url }) => ({ NAME: url.searchParams.get("name") ?? url.pathname }));

    expect(transformIndexHtml(plugin, "${NAME}", "/?name=author")).toBe("author");
    expect(transformIndexHtml(plugin, "${NAME}")).toBe("/");
  });

  it("leaves the placeholder in the HTML vite build writes", async () => {
    const root = mkdtempSync(join(tmpdir(), "serve-placeholders-"));
    temporaryRoots.push(root);
    writeFileSync(join(root, "index.html"), '<!doctype html><script id="d" type="application/json">${bookDataJson}</script>');

    await build({ root, configFile: false, logLevel: "silent", plugins: [servePlaceholders({ bookDataJson: "{}" })] });

    expect(readFileSync(join(root, "dist", "index.html"), "utf8")).toContain("${bookDataJson}");
  }, 60_000);
});
