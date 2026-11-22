import { fireEvent, screen, waitFor } from "@testing-library/react";
import { expect } from "vitest";
import de from "../../localization/de.json";
import type { VersionConflictDecision } from "./VersionConflictDialog";

const decisionButtonNames: Record<VersionConflictDecision, string> = {
  Cancel: de.cancelButton.label,
  Reload: de.versionConflict.reload,
  SaveAnyway: de.versionConflict.saveAnyway,
};

/** Answers the open `VersionConflictDialog` in German and waits until no `alertdialog` is left. */
export async function decideVersionConflict(decision: VersionConflictDecision): Promise<void> {
  fireEvent.click(await screen.findByRole("button", { name: decisionButtonNames[decision] }));
  await waitFor(() => expect(screen.queryByRole("alertdialog")).toBeNull());
}
