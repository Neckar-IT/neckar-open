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
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  resetFocusAfterRemovedElement();
  resetPointerEventsCheck();
});

/**
 * jsdom records the Document as focused element once the focused element is removed, e.g. by `cleanup()`.
 * The next `focus()` then fires `blur` at the window, and Radix Select and DropdownMenu close on window `blur`
 * right after they open. Focusing and blurring a temporary element resets the focused element to none.
 *
 * workaround: dependency=pnpm:jsdom:30.1.2 observed-on=2026-10-07 upstream=https://github.com/jsdom/jsdom/issues/4342
 */
function resetFocusAfterRemovedElement(): void {
  const placeholder = document.createElement("button");
  document.body.append(placeholder);
  placeholder.focus();
  placeholder.blur();
  placeholder.remove();
}

/**
 * user-event caches the `pointer-events` check on the element, and `document.body` outlives the test file in a worker
 * that does not isolate files; the cached `none` of a modal dialog then rejects the click of a later test.
 */
function resetPointerEventsCheck(): void {
  for (const symbol of Object.getOwnPropertySymbols(document.body)) {
    if (symbol.description === "Last check for pointer-events") {
      Reflect.deleteProperty(document.body, symbol);
    }
  }
}

/**
 * JSDOM does not support the "hasPointerCapture" which the shadcn select uses. So we need to mock it.
 */

class MockPointerEvent extends Event {
  button: number;
  ctrlKey: boolean;
  pointerType: string;

  constructor(type: string, props: PointerEventInit) {
    super(type, props);
    this.button = props.button ?? 0;
    this.ctrlKey = props.ctrlKey ?? false;
    this.pointerType = props.pointerType ?? "mouse";
  }
}
// eslint-disable-next-line @typescript-eslint/no-explicit-any
window.PointerEvent = MockPointerEvent as any;
window.HTMLElement.prototype.scrollIntoView = vi.fn();
window.HTMLElement.prototype.releasePointerCapture = vi.fn();
window.HTMLElement.prototype.hasPointerCapture = vi.fn();

/**
 * JSDOM does not implement ResizeObserver, which cmdk (the Command component) instantiates on mount.
 * Provide a no-op implementation so Command-based components render in tests.
 */

class MockResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = MockResizeObserver;

// A worker that does not isolate test files keeps the modules a file mocked in the module cache for the next file.
afterAll(() => {
  vi.resetModules();
});
