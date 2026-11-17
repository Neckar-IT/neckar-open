import type { Plugin } from "vite";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { servePlaceholders, substitutePlaceholders } from "./serve-placeholders.js";

/**
 * What {@link examplePlaceholders} serves and writes.
 */
export type ExamplePlaceholdersOptions<Contexts extends Readonly<Record<string, unknown>> = Readonly<Record<string, unknown>>> = Readonly<{
  /**
   * The examples by name; the name is the value of the query parameter `example` and the name of the written file.
   * An example is a context, or a group: an object with the key `default`, whose context is the example itself, and further keys, whose
   * contexts are its variants. The name of a variant is the value of the query parameter `variant`.
   */
  contexts: Contexts;

  /** The name of the example the dev server serves without the query parameter; a key of {@link contexts}. */
  defaultContext: keyof Contexts & string;

  /** The name of the placeholder in the HTML that receives the JSON of a context, without `${` and `}`. */
  placeholder: string;

  /** The directory of `vite build` below the project root that receives one HTML file per example. */
  outputDirectory: string;
}>;

/**
 * Fills `${<placeholder>}` while the Vite dev server serves the page with the example context named by the query parameter `example`
 * (`/?example=author`); without the parameter it is the context named `defaultContext`. The query parameter `variant` selects one of the
 * variants of the example (`/?example=author&variant=longBiography`); without the parameter it is the `default`.
 * The dev server also shows a bar with one link per example and, for an example with variants, one link per variant; `?bar=0` hides it.
 *
 * `vite build` keeps the placeholder in `dist/index.html` for the backend and writes no bar. It also writes one self-contained file
 * per example, `<outputDirectory>/<example>.html`, and one per variant, `<outputDirectory>/<example>--<variant>.html`, with the context embedded.
 * Place the plugins behind `viteSingleFile` so the files carry the inlined script and stylesheet.
 */
export function examplePlaceholders<const Contexts extends Readonly<Record<string, unknown>>>(options: ExamplePlaceholdersOptions<Contexts>): Plugin[] {
  return [
    servePlaceholders(({ url }) => ({ [options.placeholder]: JSON.stringify(selectedContext(url, options)) })),
    exampleSwitcherBar(options),
    exampleHtmlFiles(options),
  ];
}

/**
 * Writes `<example>.html` for every example into the output directory: the single-file page that `viteSingleFile` produced,
 * with the example context in place of the placeholder. Runs after `viteSingleFile` so the script and the stylesheet are inlined.
 */
function exampleHtmlFiles(options: ExamplePlaceholdersOptions): Plugin {
  let root = process.cwd();
  let html: string | undefined;

  return {
    name: "neckar-example-html-files",
    apply: "build",
    enforce: "post",

    configResolved(config) {
      root = config.root;
    },

    // `viteSingleFile` inlines in a `post` generateBundle hook; only the same order puts this one behind it, `enforce` alone does not.
    generateBundle: {
      order: "post",
      handler(_options, bundle) {
        const page = bundle["index.html"];
        if (page?.type !== "asset") {
          throw new Error("Missing asset index.html in the bundle, cannot write the example files");
        }
        html = typeof page.source === "string" ? page.source : new TextDecoder().decode(page.source);
      },
    },

    closeBundle() {
      if (html !== undefined) {
        writeExampleHtmlFiles(html, resolve(root, options.outputDirectory), options.contexts, options.placeholder);
      }
    },
  };
}

/**
 * Replaces the directory with one file per example.
 * `<` is written as `\u003c` so a value containing `</script>` or `<!--` cannot end the script element; `JSON.parse` reads it back unchanged.
 *
 * @internal exported for the test
 */
export function writeExampleHtmlFiles(html: string, directory: string, contexts: Readonly<Record<string, unknown>>, placeholder: string): void {
  rmSync(directory, { recursive: true, force: true });
  mkdirSync(directory, { recursive: true });
  const write = (fileName: string, context: unknown): void => {
    const json = JSON.stringify(context).replaceAll("<", "\\u003c");
    writeFileSync(join(directory, `${fileName}.html`), substitutePlaceholders(html, { [placeholder]: json }));
  };
  for (const [name, example] of Object.entries(contexts)) {
    write(name, defaultOf(example));
    for (const [variant, variantContext] of Object.entries(variantsOf(example))) {
      write(`${name}--${variant}`, variantContext);
    }
  }
}

/** The key of a group whose context is the example itself. */
const DefaultVariant = "default";

function isGroup(example: unknown): example is Readonly<Record<string, unknown>> {
  return typeof example === "object" && example !== null && Object.hasOwn(example, DefaultVariant);
}

function defaultOf(example: unknown): unknown {
  return isGroup(example) ? example[DefaultVariant] : example;
}

/** The variants of the example by name: the keys of a group but the `default`; empty for a context. */
function variantsOf(example: unknown): Readonly<Record<string, unknown>> {
  if (!isGroup(example)) {
    return {};
  }
  const { [DefaultVariant]: _default, ...variants } = example;
  return variants;
}

/** The context the query `example` and `variant` select. */
function selectedContext(url: URL, options: ExamplePlaceholdersOptions): unknown {
  const name = exampleName(url, options);
  const example = Object.hasOwn(options.contexts, name) ? options.contexts[name] : undefined;
  if (example === undefined) {
    throw new Error(`Unknown example "${name}", expected one of: ${Object.keys(options.contexts).join(", ")}`);
  }
  const variant = variantName(url);
  if (variant === DefaultVariant) {
    return defaultOf(example);
  }
  const variants = variantsOf(example);
  const context = Object.hasOwn(variants, variant) ? variants[variant] : undefined;
  if (context === undefined) {
    throw new Error(`Unknown variant "${variant}" of example "${name}", expected one of: ${[DefaultVariant, ...Object.keys(variants)].join(", ")}`);
  }
  return context;
}

function variantName(url: URL): string {
  return url.searchParams.get("variant") ?? DefaultVariant;
}

function exampleName(url: URL, options: ExamplePlaceholdersOptions): string {
  return url.searchParams.get("example") ?? options.defaultContext;
}

function exampleSwitcherBar(options: ExamplePlaceholdersOptions): Plugin {
  return {
    name: "neckar-example-switcher-bar",
    apply: "serve",

    transformIndexHtml(_html, ctx) {
      const url = new URL(ctx?.originalUrl ?? "/", "http://localhost");
      if (url.searchParams.get("bar") === "0") {
        return [];
      }
      return [{ tag: "div", attrs: { id: "example-switcher-bar" }, children: exampleSwitcherBarShadowRoot(url, options), injectTo: "body" }];
    },
  };
}

/**
 * The bar is a declarative shadow root: its styles stay out of the page, and `position: fixed` keeps it out of the document flow,
 * so `scrollHeight` of the page containers stays the same. It is hidden when printing, so it never reaches a PDF.
 */
function exampleSwitcherBarShadowRoot(url: URL, options: ExamplePlaceholdersOptions): string {
  const current = exampleName(url, options);
  const exampleLinks = Object.keys(options.contexts).map((name) => barLink(url, name, { example: name, variant: undefined }, name === current));
  const variantNames = Object.keys(variantsOf(options.contexts[current]));
  // An example without variants shows no variant links: `default` alone would be a link to the page itself
  const variantLinks =
    variantNames.length === 0
      ? ""
      : `<i></i><span>Variant</span>${[DefaultVariant, ...variantNames]
          .map((name) => barLink(url, name, { example: current, variant: name === DefaultVariant ? undefined : name }, name === variantName(url)))
          .join("")}`;
  return `<template shadowrootmode="open"><style>${BarStyles}</style><nav><span>Example</span>${exampleLinks.join("")}${variantLinks}</nav></template>`;
}

/** A link that sets `example` and sets or removes `variant` and keeps every other query parameter. */
function barLink(url: URL, label: string, target: { example: string; variant: string | undefined }, isCurrent: boolean): string {
  const targetUrl = new URL(url);
  targetUrl.searchParams.set("example", target.example);
  if (target.variant === undefined) {
    targetUrl.searchParams.delete("variant");
  } else {
    targetUrl.searchParams.set("variant", target.variant);
  }
  const href = `${targetUrl.pathname}${targetUrl.search}`;
  return `<a href="${escapeHtml(href)}"${isCurrent ? ' aria-current="page"' : ""}>${escapeHtml(label)}</a>`;
}

const BarStyles =
  "nav{position:fixed;bottom:.75rem;left:50%;transform:translateX(-50%);z-index:2147483647;display:flex;gap:.25rem;padding:.25rem;" +
  "background:rgb(19 21 26);border:1px solid rgb(52 56 65);border-radius:9999px;box-shadow:0 0 .5rem rgb(0 0 0 / .5);" +
  "font:12px/1 system-ui,-apple-system,Segoe UI,sans-serif}" +
  "span{color:rgb(130 134 148);padding:.5rem .25rem .5rem .75rem}" +
  'span::after{content:":"}' +
  "i{align-self:stretch;width:1px;margin:0 .25rem;background:rgb(52 56 65)}" +
  "a{color:rgb(191 193 201);text-decoration:none;padding:.5rem .75rem;border-radius:9999px;transition:background .15s,color .15s}" +
  "a:hover{background:rgb(34 38 46);color:rgb(255 255 255)}" +
  'a[aria-current="page"]{background:rgb(61 68 82);color:rgb(255 255 255)}' +
  "@media print{:host{display:none}}";

function escapeHtml(text: string): string {
  return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}
