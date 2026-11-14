/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
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
});
