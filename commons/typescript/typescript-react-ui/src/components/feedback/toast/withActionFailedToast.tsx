import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import { TranslateEmbedded } from "../../../localization/TranslateEmbedded";
import { showUnknownOutcomeToast } from "../unknown-outcome/UnknownOutcomeAlert";
import { recordUnknownOutcome } from "../unknown-outcome/unknownOutcomeStore";
import { mutationFailedToastId, toast } from "./toast";

/**
 * Awaits the mutation of one action and rejects with its error. A rejection shows the `actionFeedback.failed` toast naming
 * `actionLabel`; an unknown outcome shows `UnknownOutcomeAlert` in the form shell that is submitting, or its toast when
 * no shell is. A mutation the service did not run, a cancelled one and an ended session leave the toast to
 * `createMutationErrorToastHandler`.
 */
export async function withActionFailedToast<T>(actionLabel: string, mutation: Promise<T>): Promise<T> {
  try {
    return await mutation;
  } catch (error) {
    showFailure(actionLabel, error);
    throw error;
  }
}

function showFailure(actionLabel: string, error: unknown): void {
  const reaction = reactionToFailure(error, "mutation");
  switch (reaction.type) {
    case "unknownOutcome":
      if (!recordUnknownOutcome(actionLabel)) {
        showUnknownOutcomeToast(<TranslateEmbedded textKey={{ key: "unknownOutcome.title", options: { action: actionLabel } }} />);
      }
      return;
    case "cancelled":
    case "notExecuted":
    case "sessionExpired":
      return;
    case "permissionDenied":
    case "versionMismatch":
    case "errorPage":
      toast.error(<TranslateEmbedded textKey={{ key: "actionFeedback.failed", options: { action: actionLabel } }} />, {
        id: mutationFailedToastId,
      });
      return;
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(reaction)}`);
  }
}
