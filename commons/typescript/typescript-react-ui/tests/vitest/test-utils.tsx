/**
 * Custom render function that wraps components with all required providers.
 * Import from "@test/test-utils" instead of "@testing-library/react".
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { NowProvider } from "@neckar-hub/typescript-react-utils/providers";
import { render, type RenderOptions, type RenderResult } from "@testing-library/react";
import type { ReactElement, ReactNode } from "react";

function AllProviders({ children }: { children: ReactNode }) {
  return (
    <I18nProvider>
      <NowProvider>{children}</NowProvider>
    </I18nProvider>
  );
}

function renderWithProviders(ui: ReactElement, options?: Omit<RenderOptions, "wrapper">): RenderResult {
  return render(ui, { wrapper: AllProviders, ...options });
}

/**
 * Picks one element out of a `getAllBy*` result. Negative indices count from the end,
 * so `elementAt(buttons, -1)` is the last button.
 *
 * Fails the test with a descriptive error when the queried element does not exist,
 * instead of handing `undefined` to `fireEvent` / `userEvent`.
 */
function elementAt<T>(elements: readonly T[], index: number): T {
  const element = elements.at(index);

  if (element === undefined) {
    throw new Error(`Expected an element at index [${index}], but only [${elements.length}] were rendered`);
  }

  return element;
}

/**
 * The tooltip trigger `DisabledReasonTooltip` wraps around a disabled button, which takes no focus and no pointer events.
 */
function tooltipTriggerOf(button: HTMLElement): HTMLElement {
  const wrapper = button.parentElement;

  if (wrapper === null) {
    throw new Error("Expected a tooltip wrapper around the button, but it has no parent");
  }

  return wrapper;
}

export * from "@testing-library/react";
export { elementAt, renderWithProviders as render, tooltipTriggerOf };
