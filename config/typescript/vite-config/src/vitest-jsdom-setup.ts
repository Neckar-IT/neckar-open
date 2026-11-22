import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, vi } from "vitest";

/**
 * ```ts
 * setupFiles: ["@neckar-hub/vite-config/vitest-jsdom-setup", "./tests/vitest/.vitest/vitest-setup.ts"],
 * ```
 */

afterEach(() => {
  cleanup();
  resetFocusAfterRemovedElement();
});

// `isolate: false` in `sharedVitestTestOptions` lets the test files of a package share one worker, which keeps
// the modules a file mocked in the module cache for the next file.
afterAll(() => {
  vi.resetModules();
});

// workaround: dependency=pnpm:jsdom:30.1.2 observed-on=2026-10-07 upstream=https://github.com/jsdom/jsdom/issues/4342
// After a focused element is removed, the next `focus()` fires `blur` at the window, which closes Radix Select and DropdownMenu.
function resetFocusAfterRemovedElement(): void {
  const placeholder = document.createElement("button");
  document.body.append(placeholder);
  placeholder.focus();
  placeholder.blur();
  placeholder.remove();
}

/**
 * jsdom lacks `scrollIntoView` and the pointer capture the shadcn select calls.
 */
window.HTMLElement.prototype.scrollIntoView = vi.fn();
window.HTMLElement.prototype.releasePointerCapture = vi.fn();
window.HTMLElement.prototype.hasPointerCapture = vi.fn();

/**
 * jsdom lacks `ResizeObserver`, which Radix-based fields (date picker, popovers) observe on mount.
 */
class MockResizeObserver {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
window.ResizeObserver = MockResizeObserver as any;
