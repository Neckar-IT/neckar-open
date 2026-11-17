import { createEntityDialogAtom, useOpenEntityDialog } from "../overlays/entity-dialog";
import { type VersionConflictDecision, VersionConflictDialog } from "./VersionConflictDialog";

interface VersionConflictPrompt {
  resolve: (decision: VersionConflictDecision) => void;
}

const versionConflictPrompt = createEntityDialogAtom<VersionConflictPrompt>(({ entity, onOpenChange }) => (
  <VersionConflictDialog
    onDecision={(decision) => {
      entity.resolve(decision);
      onOpenChange(false);
    }}
  />
));

/**
 * Shows the {@link VersionConflictDialog} through `<DialogRegistry />` and answers the decision the user clicks;
 * closing the dialog answers `Cancel`.
 */
export function useAskVersionConflictDecision(): () => Promise<VersionConflictDecision> {
  const open = useOpenEntityDialog(versionConflictPrompt);
  return () => new Promise((resolve) => open({ resolve }));
}
