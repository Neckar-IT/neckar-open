import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { UsedByCount } from "./CascadingDraftResetWarningDialog";
import { useAskCascadingResetDecision } from "./useAskCascadingResetDecision";

/** A draft-confirmed entity with the dependents a save would reset. */
export interface HasConfirmationAndUsedBy {
  confirmationStatus: ConfirmationStatus;
  usedBy: { counts: UsedByCount[] };
}

/**
 * Answers whether a save of `entity` may go ahead: `undefined` (not stored yet) and a Draft save at once,
 * every other status asks the cascading reset warning, because the save turns it and its dependents back into Drafts.
 */
export function useConfirmCascadingReset(entity: HasConfirmationAndUsedBy | undefined): () => Promise<boolean> {
  const askCascadingResetDecision = useAskCascadingResetDecision();
  return async () =>
    entity === undefined ||
    entity.confirmationStatus === ConfirmationStatus.Draft ||
    (await askCascadingResetDecision(entity.usedBy.counts)) === "SaveAnyway";
}
