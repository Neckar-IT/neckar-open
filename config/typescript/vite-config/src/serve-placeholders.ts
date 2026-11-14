import type { Plugin } from "vite";

/**
 * What each placeholder is replaced with. `undefined` leaves the placeholder in the HTML.
 */
export type PlaceholderValues = Readonly<Record<string, string | undefined>>;

/**
 * What a values function learns about the request being served.
 */
export type PlaceholderRequest = Readonly<{
  /**
   * The URL of the request, with its query parameters.
   */
  url: URL;
}>;

/**
 * Where the plugin takes its values from: a fixed table, or a function evaluated at each request so the values can follow the environment and the request URL.
 */
export type PlaceholderValuesSource = PlaceholderValues | ((request: PlaceholderRequest) => PlaceholderValues);

/**
 * Replaces `${NAME}` in the HTML while the Vite dev server serves it, for every name the values carry.
 *
 * Serve mode only, and that is the load-bearing part: `vite build` has to write the placeholders into
 * `dist/index.html` untouched, because the consumer of the file (an nginx entrypoint, a backend) replaces them
 * with the values of its own environment.
 *
 * @param order When the hook runs relative to the other `transformIndexHtml` hooks; `post` runs after hooks that add HTML carrying placeholders.
 */
export function servePlaceholders(values: PlaceholderValuesSource, order?: "pre" | "post"): Plugin {
  return {
    name: "neckar-serve-placeholders",
    apply: "serve",

    transformIndexHtml: {
      order,

      handler(html, ctx) {
        const url = new URL(ctx?.originalUrl ?? "/", "http://localhost");
        return substitutePlaceholders(html, typeof values === "function" ? values({ url }) : values);
      },
    },
  };
}

/**
 * Replaces `${NAME}` for every name the values carry, and nothing else — `envsubst` with an explicit
 * variable list behaves the same way, and an HTML file may contain other `$…`.
 *
 * The replacement is a function because a plain string one lets `$&` and its siblings inside a value
 * expand, which would make a value with a `$` in it read differently here than under `envsubst`.
 */
export function substitutePlaceholders(html: string, values: PlaceholderValues): string {
  return Object.entries(values).reduce(
    (substituted, [name, value]) => (value === undefined ? substituted : substituted.replaceAll(`\${${name}}`, () => value)),
    html,
  );
}
