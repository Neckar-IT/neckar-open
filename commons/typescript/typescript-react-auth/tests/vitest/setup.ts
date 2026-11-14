/**
 * Imports
 */

import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, vi } from "vitest";

/**
 * Setup Tests
 */

// run a cleanup after each test
afterEach(() => {
  cleanup();
});

/**
 * JSDOM implements neither ResizeObserver nor scrollIntoView, which cmdk (the combobox of the impersonation dialog) calls on mount.
 */

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = MockResizeObserver;
window.HTMLElement.prototype.scrollIntoView = vi.fn();

// A worker that does not isolate test files keeps the modules a file mocked in the module cache for the next file.
afterAll(() => {
  vi.resetModules();
});
