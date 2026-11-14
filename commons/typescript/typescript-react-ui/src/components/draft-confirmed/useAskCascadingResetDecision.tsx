import { createEntityDialogAtom, useOpenEntityDialog } from "../overlays/entity-dialog";
import { CascadingDraftResetWarningDialog, type UsedByCount } from "./CascadingDraftResetWarningDialog";
import type { CascadingResetDecision } from "./cascadingResetDecision";

interface CascadingResetPrompt {
  usedByCounts: UsedByCount[];
  resolve: (decision: CascadingResetDecision) => void;
}

const cascadingResetPrompt = createEntityDialogAtom<CascadingResetPrompt>(({ entity, onOpenChange }) => (
  <CascadingDraftResetWarningDialog
    usedByCounts={entity.usedByCounts}
    onOpenChange={(isOpen) => {
      if (isOpen) return;
      entity.resolve("Cancel");
      onOpenChange(false);
    }}
    onSubmit={() => {
      entity.resolve("SaveAnyway");
      onOpenChange(false);
    }}
  />
));

/**
 * Shows the {@link CascadingDraftResetWarningDialog} through `<DialogRegistry />` and answers the decision the
 * user clicks; closing the dialog answers `Cancel`.
 */
export function useAskCascadingResetDecision(): (usedByCounts: UsedByCount[]) => Promise<CascadingResetDecision> {
  const open = useOpenEntityDialog(cascadingResetPrompt);
  return (usedByCounts) => new Promise((resolve) => open({ usedByCounts, resolve }));
}
