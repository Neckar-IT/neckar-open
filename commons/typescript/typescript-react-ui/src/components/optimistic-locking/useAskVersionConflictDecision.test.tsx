/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { act, render, renderHook, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createStore, Provider } from "jotai";
import { describe, expect, it } from "vitest";
import { DialogRegistry } from "../overlays/entity-dialog";
import { useAskVersionConflictDecision } from "./useAskVersionConflictDecision";
import type { VersionConflictDecision } from "./VersionConflictDialog";

/**
 * Tests
 *
 * useAskVersionConflictDecision shows the version conflict dialog in the DialogRegistry and answers the
 * clicked decision.
 */

function renderPrompt(): () => Promise<VersionConflictDecision> {
  const store = createStore();
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <I18nProvider>
      <Provider store={store}>{children}</Provider>
    </I18nProvider>
  );
  render(<DialogRegistry />, { wrapper });
  return renderHook(() => useAskVersionConflictDecision(), { wrapper }).result.current;
}

describe("useAskVersionConflictDecision", () => {
  it("answers the decision the user clicks and hides the dialog", async () => {
    const user = userEvent.setup();
    const ask = renderPrompt();

    let decision: Promise<VersionConflictDecision> = Promise.resolve("Cancel");
    act(() => {
      decision = ask();
    });
    await user.click(await screen.findByRole("button", { name: "Save anyway" }));

    await expect(decision).resolves.toBe("SaveAnyway");
    expect(screen.queryByRole("button", { name: "Save anyway" })).not.toBeInTheDocument();
  });

  it("answers Cancel when the user closes the dialog", async () => {
    const user = userEvent.setup();
    const ask = renderPrompt();

    let decision: Promise<VersionConflictDecision> = Promise.resolve("SaveAnyway");
    act(() => {
      decision = ask();
    });
    await screen.findByRole("button", { name: "Save anyway" });
    await user.keyboard("{Escape}");

    await expect(decision).resolves.toBe("Cancel");
  });
});
