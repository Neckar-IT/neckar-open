import { mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { HtmlTagDescriptor, Plugin } from "vite";
import { describe, expect, it } from "vitest";
import { examplePlaceholders, writeExampleHtmlFiles } from "./example-placeholders.js";

const contexts = { first: { data: { type: "first" } }, second: { data: { type: "second" } } };
const options = { contexts, defaultContext: "first", placeholder: "injectedContext", outputDirectory: "build/generated/pdf-examples" } as const;

function plugins(): Plugin[] {
  return examplePlaceholders(options);
}

type TransformIndexHtmlHook = { handler: (html: string, ctx: { originalUrl: string }) => string };

function serve(originalUrl: string): string {
  const placeholders = plugins()[0] as Plugin;
  return (placeholders.transformIndexHtml as TransformIndexHtmlHook).handler("${injectedContext}", { originalUrl });
}

function barTags(originalUrl: string): HtmlTagDescriptor[] {
  const bar = plugins()[1] as Plugin;
  const hook = bar.transformIndexHtml as (html: string, ctx: { originalUrl: string }) => HtmlTagDescriptor[];
  return hook("", { originalUrl });
}

function barHtml(originalUrl: string): string {
  return barTags(originalUrl)
    .map((tag) => (typeof tag.children === "string" ? tag.children : ""))
    .join("");
}

describe("examplePlaceholders", () => {
  it("serves the default example without the parameter", () => {
    expect(serve("/")).toBe(JSON.stringify(contexts.first));
  });

  it("serves the example named by the parameter", () => {
    expect(serve("/?example=second")).toBe(JSON.stringify(contexts.second));
  });

  it("rejects an unknown example and names the known ones", () => {
    expect(() => serve("/?example=unknown")).toThrow('Unknown example "unknown", expected one of: first, second');
  });

  it("accepts only a key of the contexts as the default context", () => {
    // @ts-expect-error "missing" is no key of `contexts`
    examplePlaceholders({ ...options, defaultContext: "missing" });
  });

  it("rejects a name inherited from Object", () => {
    expect(() => serve("/?example=toString")).toThrow("expected one of: first, second");
  });
});

describe("variants", () => {
  const wide = { data: { type: "first", wide: true } };
  const narrow = { data: { type: "first", narrow: true } };
  const groups = { first: { default: contexts.first, wide, narrow }, second: contexts.second };
  const withGroups = { ...options, contexts: groups };

  function serveGroups(originalUrl: string): string {
    const placeholders = examplePlaceholders(withGroups)[0] as Plugin;
    return (placeholders.transformIndexHtml as TransformIndexHtmlHook).handler("${injectedContext}", { originalUrl });
  }

  function groupsBarHtml(originalUrl: string): string {
    const bar = examplePlaceholders(withGroups)[1] as Plugin;
    const hook = bar.transformIndexHtml as (html: string, ctx: { originalUrl: string }) => HtmlTagDescriptor[];
    return hook("", { originalUrl })
      .map((tag) => (typeof tag.children === "string" ? tag.children : ""))
      .join("");
  }

  it("serves the default of a group without the parameters", () => {
    expect(serveGroups("/")).toBe(JSON.stringify(contexts.first));
    expect(serveGroups("/?example=first")).toBe(JSON.stringify(contexts.first));
  });

  it("serves the variant named by the parameter", () => {
    expect(serveGroups("/?example=first&variant=wide")).toBe(JSON.stringify(wide));
  });

  it("serves the default of a group for the variant default", () => {
    expect(serveGroups("/?example=first&variant=default")).toBe(JSON.stringify(contexts.first));
  });

  it("serves a context without variants as before", () => {
    expect(serveGroups("/?example=second")).toBe(JSON.stringify(contexts.second));
  });

  it("rejects an unknown variant and names the known ones", () => {
    expect(() => serveGroups("/?example=first&variant=x")).toThrow('Unknown variant "x" of example "first", expected one of: default, wide, narrow');
  });

  it("rejects a variant of a context without variants", () => {
    expect(() => serveGroups("/?example=second&variant=wide")).toThrow('Unknown variant "wide" of example "second", expected one of: default');
  });

  it("rejects a variant name inherited from Object", () => {
    expect(() => serveGroups("/?example=first&variant=toString")).toThrow("expected one of: default, wide, narrow");
  });

  it("links the examples and the variants of the selected example in one bar", () => {
    const html = groupsBarHtml("/?example=first&variant=wide");
    expect(html.match(/<nav>/g)).toHaveLength(1);
    expect(html).toContain("<span>Example</span>");
    expect(html).toContain("<span>Variant</span>");
    expect(html).toContain('<a href="/?example=first">default</a>');
    expect(html).toContain('<a href="/?example=first&amp;variant=wide" aria-current="page">wide</a>');
    expect(html).toContain('<a href="/?example=first&amp;variant=narrow">narrow</a>');
    expect(html).toContain('<a href="/?example=second">second</a>');
  });

  it("separates the examples from the variants", () => {
    expect(groupsBarHtml("/?example=first")).toContain('<a href="/?example=second">second</a><i></i><span>Variant</span>');
    expect(groupsBarHtml("/?example=second")).not.toContain("<i></i>");
  });

  it("marks default among the variants without the parameter", () => {
    expect(groupsBarHtml("/?example=first")).toContain('<a href="/?example=first" aria-current="page">default</a>');
  });

  it("drops the variant when another example is selected", () => {
    expect(groupsBarHtml("/?example=first&variant=wide")).toContain('<a href="/?example=second">second</a>');
  });

  it("shows no variants for a context", () => {
    expect(groupsBarHtml("/?example=second")).not.toContain("<span>Variant</span>");
    expect(barHtml("/")).not.toContain("<span>Variant</span>");
  });

  it("writes one file per example and variant", () => {
    const directory = mkdtempSync(join(tmpdir(), "pdf-examples-"));
    try {
      writeExampleHtmlFiles("${injectedContext}", join(directory, "out"), groups, "injectedContext");
      expect(readdirSync(join(directory, "out")).toSorted()).toEqual(["first--narrow.html", "first--wide.html", "first.html", "second.html"]);
      expect(readFileSync(join(directory, "out", "first.html"), "utf8")).toBe(JSON.stringify(contexts.first));
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  });
});

describe("example switcher bar", () => {
  it("applies in serve mode only", () => {
    expect((plugins()[1] as Plugin).apply).toBe("serve");
  });

  it("links every example", () => {
    const html = barHtml("/");
    for (const name of Object.keys(contexts)) {
      expect(html).toContain(`<a href="/?example=${name}"`);
    }
  });

  it("marks the example named by the parameter", () => {
    expect(barHtml("/?example=second")).toContain('<a href="/?example=second" aria-current="page">second</a>');
    expect(barHtml("/?example=second")).not.toContain('<a href="/?example=first" aria-current="page">');
  });

  it("marks the default example without the parameter", () => {
    expect(barHtml("/")).toContain('<a href="/?example=first" aria-current="page">first</a>');
  });

  it("keeps the other query parameters", () => {
    expect(barHtml("/?example=second&foo=1&bar=1")).toContain('<a href="/?example=first&amp;foo=1&amp;bar=1">first</a>');
  });

  it("labels the bar", () => {
    expect(barHtml("/")).toContain("<nav><span>Example</span>");
  });

  it("is hidden by bar=0", () => {
    expect(barTags("/?bar=0")).toEqual([]);
  });

  it("is a fixed shadow root", () => {
    const html = barHtml("/");
    expect(html).toContain('<template shadowrootmode="open">');
    expect(html).toContain("position:fixed");
  });

  it("is hidden when printing", () => {
    expect(barHtml("/")).toContain("@media print{:host{display:none}}");
  });
});

describe("example HTML files", () => {
  const page = '<html><body><script id="injectedContext" type="application/json">${injectedContext}</script></body></html>';

  function withDirectory(run: (directory: string) => void): void {
    const directory = mkdtempSync(join(tmpdir(), "pdf-examples-"));
    try {
      run(join(directory, "pdf-examples"));
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  }

  function injected(html: string): string {
    const match = /<script id="injectedContext" type="application\/json">(.*)<\/script><\/body>/s.exec(html);
    return match?.[1] ?? "";
  }

  it("builds only", () => {
    const plugin = plugins()[2] as Plugin;
    expect(plugin.apply).toBe("build");
    expect(plugin.enforce).toBe("post");
    expect(plugin.generateBundle).toMatchObject({ order: "post" });
  });

  it("writes one file per example with its render context", () => {
    withDirectory((directory) => {
      writeExampleHtmlFiles(page, directory, contexts, "injectedContext");
      expect(readdirSync(directory).toSorted()).toEqual(Object.keys(contexts).map((name) => `${name}.html`).toSorted());
      for (const [name, context] of Object.entries(contexts)) {
        const html = readFileSync(join(directory, `${name}.html`), "utf8");
        expect(JSON.parse(injected(html))).toEqual(JSON.parse(JSON.stringify(context)));
        expect(html).not.toContain("${injectedContext}");
        expect(html).not.toContain("example-switcher-bar");
      }
    });
  });

  it("masks a closing script tag in a value", () => {
    withDirectory((directory) => {
      writeExampleHtmlFiles(page, directory, { tricky: { title: "</script><!--" } }, "injectedContext");
      const html = readFileSync(join(directory, "tricky.html"), "utf8");
      expect(html).toContain("\\u003c/script>\\u003c!--");
      expect((JSON.parse(injected(html)) as { title: string }).title).toBe("</script><!--");
    });
  });

  it("removes a stale file", () => {
    withDirectory((directory) => {
      mkdirSync(directory, { recursive: true });
      writeFileSync(join(directory, "removed.html"), "");
      writeExampleHtmlFiles(page, directory, contexts, "injectedContext");
      expect(readdirSync(directory)).not.toContain("removed.html");
    });
  });
});
