/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { VersionConflictDialog } from "./VersionConflictDialog";

/**
 * Tests
 *
 * VersionConflictDialog offers cancel plus the resolutions it is given and reports the one the user
 * picks; each text can be replaced on its own.
 */

describe("VersionConflictDialog", () => {
  it("reports each decision the user picks", async () => {
    const user = userEvent.setup();
    const onDecision = vi.fn();
    render(<VersionConflictDialog onDecision={onDecision} />);

    await user.click(screen.getByRole("button", { name: "Discard and reload" }));
    await user.click(screen.getByRole("button", { name: "Save anyway" }));
    await user.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onDecision.mock.calls).toEqual([["Reload"], ["SaveAnyway"], ["Cancel"]]);
  });

  it("offers only the resolutions it is given", () => {
    render(<VersionConflictDialog onDecision={vi.fn()} resolutions={["Reload"]} />);

    expect(screen.getByRole("button", { name: "Discard and reload" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Save anyway" })).not.toBeInTheDocument();
  });

  it("replaces a single text and keeps the shared ones", () => {
    render(
      <VersionConflictDialog onDecision={vi.fn()} messages={{ title: "Book changed meanwhile" }}>
        <p>Title: Changed by somebody else</p>
      </VersionConflictDialog>,
    );

    expect(screen.getByText("Book changed meanwhile")).toBeInTheDocument();
    expect(screen.getByText("Somebody saved this entry while you were editing it. Your changes are not saved yet.")).toBeInTheDocument();
    expect(screen.getByText("Title: Changed by somebody else")).toBeInTheDocument();
  });
});
