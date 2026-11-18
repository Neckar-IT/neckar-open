import { ArchiveState } from "@neckar-hub/typescript-utils/openapi-types";
import type { Action } from "../../actions/Action";

/**
 * Picks the archive action for an active entity and the unarchive action for an archived one. Takes both actions so
 * the caller calls both hooks on every render and the hook order stays fixed while the archive state changes.
 */
export function actionForArchiveState(archiveState: ArchiveState, actions: { archive: Action; unarchive: Action }): Action {
  return archiveState === ArchiveState.Active ? actions.archive : actions.unarchive;
}
