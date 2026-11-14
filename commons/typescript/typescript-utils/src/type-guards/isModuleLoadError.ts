/**
 * Message prefixes of the `TypeError` a dynamic `import()` rejects with when the chunk does not load, per browser.
 * The browsers append the chunk URL.
 */
const ModuleLoadErrorMessagePrefixes = [
  "Failed to fetch dynamically imported module", // Chromium
  "error loading dynamically imported module", // Firefox
  "Importing a module script failed", // Safari
];

export function isModuleLoadError(error: unknown): boolean {
  return error instanceof TypeError && ModuleLoadErrorMessagePrefixes.some((prefix) => error.message.startsWith(prefix));
}
