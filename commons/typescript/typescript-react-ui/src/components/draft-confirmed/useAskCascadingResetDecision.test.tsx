/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { act, render, renderHook, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createStore, Provider } from "jotai";
import { describe, expect, it } from "vitest";
import { DialogRegistry } from "../overlays/entity-dialog";
import type { CascadingResetDecision } from "./cascadingResetDecision";
import { useAskCascadingResetDecision } from "./useAskCascadingResetDecision";

/**
 * Tests
 *
 * useAskCascadingResetDecision shows the cascading draft reset warning in the DialogRegistry and answers the
 * clicked decision.
 */

function renderPrompt(): (usedByCounts: { type: string; activeConfirmedCount?: number | null }[]) => Promise<CascadingResetDecision> {
  const store = createStore();
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <I18nProvider>
      <Provider store={store}>{children}</Provider>
    </I18nProvider>
  );
  render(<DialogRegistry />, { wrapper });
  return renderHook(() => useAskCascadingResetDecision(), { wrapper }).result.current;
}

describe("useAskCascadingResetDecision", () => {
  it("answers SaveAnyway when the user saves anyway", async () => {
    const user = userEvent.setup();
    const ask = renderPrompt();

    let decision: Promise<CascadingResetDecision> = Promise.resolve("Cancel");
    act(() => {
      decision = ask([{ type: "Book", activeConfirmedCount: 2 }]);
    });
    await user.click(await screen.findByRole("button", { name: "Save anyway" }));

    await expect(decision).resolves.toBe("SaveAnyway");
    expect(screen.queryByRole("button", { name: "Save anyway" })).not.toBeInTheDocument();
  });

  it("answers Cancel when the user cancels", async () => {
    const user = userEvent.setup();
    const ask = renderPrompt();

    let decision: Promise<CascadingResetDecision> = Promise.resolve("SaveAnyway");
    act(() => {
      decision = ask([]);
    });
    await user.click(await screen.findByRole("button", { name: "Cancel" }));

    await expect(decision).resolves.toBe("Cancel");
  });
});
