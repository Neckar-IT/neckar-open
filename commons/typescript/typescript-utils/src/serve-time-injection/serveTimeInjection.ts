/**
 * Reads values that the nginx entrypoint `40-inject-serve-time-values.sh` substitutes into the served
 * HTML at container start.
 *
 * The script fills `${…}` placeholders via `envsubst`. A `<meta>` tag is the only carrier this module
 * reads: where a frontend sends `script-src 'self'` without `'unsafe-inline'`, an inline `<script>`
 * writing a global is parsed into the DOM and never executed, so a global reads as absent on exactly
 * the deployments that matter.
 *
 * The values deliberately never enter the Vite build: a build-time `import.meta.env.VITE_…` puts
 * the deploy metadata into the bundle input and costs every frontend package its up-to-date state
 * on every commit.
 *
 * Two categories travel this channel — deploy metadata (the digest of the image served) and
 * runtime context (which deployment the container runs in). They are defined in the Serve-Time
 * Injection section of https://glossary.neckar.it — this module is the mechanism both share.
 */

/**
 * Reads one serve-time injected `<meta>` tag by its `name` attribute.
 *
 * The general reader: every specific value of both categories is a named tag resolved through here.
 *
 * Returns `undefined` outside a DOM (a plain vitest run), when no such tag exists, or when the
 * content is absent per {@link sanitizeInjectedValue}.
 */
export function readInjectedMeta(metaName: string): string | undefined {
  if (typeof document === "undefined") {
    return undefined;
  }

  const content = document.querySelector(`meta[name='${metaName}']`)?.getAttribute("content");
  return content === null || content === undefined ? undefined : sanitizeInjectedValue(content);
}

/**
 * Treats an empty string (envsubst ran without the env var) and an unreplaced `${…}` placeholder
 * (HTML served without the injecting entrypoint, e.g. the local Vite dev server) as absent.
 */
export function sanitizeInjectedValue(value: string): string | undefined {
  return value.length === 0 || value.startsWith("${") ? undefined : value;
}
