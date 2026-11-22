/**
 * Imports
 */

import { act, elementAt, render, screen, tooltipTriggerOf } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { ActionLockProvider } from "../actions/ActionLockProvider";
import { CascadingDraftResetWarningDialog } from "./CascadingDraftResetWarningDialog";

/**
 * Tests
 *
 * CascadingDraftResetWarningDialog describes the reset in one sentence and lists the entities with confirmed records
 * outside the description element.
 */

describe("CascadingDraftResetWarningDialog", () => {
  it("lists the entities with confirmed records outside the description", () => {
    render(
      <CascadingDraftResetWarningDialog
        usedByCounts={[{ type: "Book", activeConfirmedCount: 3 }]}
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    const item = screen.getByRole("listitem");
    expect(item.closest("[data-slot='alert-dialog-description']")).toBeNull();
  });

  it("renders no list when no entity has confirmed records", () => {
    render(
      <CascadingDraftResetWarningDialog
        usedByCounts={[{ type: "Book", activeConfirmedCount: 0 }]}
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("disables save anyway inside an ActionLockProvider and names the reason", async () => {
    render(
      <ActionLockProvider reason="Gerät offline">
        <CascadingDraftResetWarningDialog usedByCounts={[]} onOpenChange={vi.fn()} onSubmit={vi.fn()} />
      </ActionLockProvider>,
    );
    const saveAnyway = elementAt(screen.getAllByRole("button"), -1);

    // Focus skips the hover delay, which the modal dialog in jsdom can stretch past the timeout of findByRole.
    act(() => tooltipTriggerOf(saveAnyway).focus());

    expect(saveAnyway).toBeDisabled();
    expect(await screen.findByRole("tooltip")).toHaveTextContent("Gerät offline");
  });
});
