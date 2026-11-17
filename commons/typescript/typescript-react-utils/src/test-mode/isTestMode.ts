/**
 * Extend the window type
 */

declare global {
  interface Window {
    testMode: boolean | undefined;
  }
}

/**
 * Name of the window property that carries the test mode.
 *
 * `setTestMode` writes exactly this property — the two sides run in different processes, so a
 * literal on each side drifts apart unnoticed (it did: a directory rename to kebab-case renamed the
 * property to `test-mode` on the writing side and left every screenshot test without a test mode).
 */

export const testModeWindowProperty = "testMode";

/**
 * Read the `testMode` property from the global window object-
 */

export function isTestMode(): boolean {
  return window[testModeWindowProperty] ?? false;
}
