/**
 * Imports
 */

import { act, cleanup, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { createEntityDialogAtom, useOpenEntityDialog } from "./entity-dialog/entityDialogAtom";
import { renderWithDialogs } from "./overlaysTestSupport";

/**
 * Tests
 */

const dialogAtom = createEntityDialogAtom<string>(({ entity }) => <p>{`Archive ${entity}`}</p>);

function OpenButton(): React.ReactElement {
  const openDialog = useOpenEntityDialog(dialogAtom);
  return (
    <button type="button" onClick={() => openDialog("Emma")}>
      Open
    </button>
  );
}

describe("renderWithDialogs", () => {
  it("starts every render without the dialog an earlier render opened", () => {
    renderWithDialogs(<OpenButton />);
    act(() => screen.getByRole("button", { name: "Open" }).click());
    expect(screen.getByText("Archive Emma")).toBeInTheDocument();

    cleanup();
    renderWithDialogs(<OpenButton />);

    expect(screen.queryByText("Archive Emma")).toBeNull();
  });
});
