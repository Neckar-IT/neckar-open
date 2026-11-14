import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import { mutationFailedToastId, toast } from "../components/feedback";
import { showUnknownOutcomeToast } from "../components/feedback/unknown-outcome/UnknownOutcomeAlert";
import { recordUnknownOutcome } from "../components/feedback/unknown-outcome/unknownOutcomeStore";
import { TranslateEmbedded } from "../localization/TranslateEmbedded";

export interface MutationErrorToastHandlerOptions {
  /**
   * Optional error reporter, e.g. `(error) => captureError(error, "mutation")` from
   * `typescript-otel-tracing`. Kept as a callback so this package stays free of an OTel dependency.
   */
  report?: (error: unknown) => void;
}

/**
 * Handler for `createQueryClient({ onMutationError })`: reports every failed mutation, also one whose promise the caller
 * discards, and shows the toast or `UnknownOutcomeAlert` of its `reactionToFailure`. Needs the `ToastProvider`.
 *
 * ```tsx
 * createQueryClient({ onMutationError: createMutationErrorToastHandler({ report: (error) => captureError(error, "mutation") }) });
 * ```
 */
export function createMutationErrorToastHandler(options: MutationErrorToastHandlerOptions = {}): (error: unknown) => void {
  return (error) => {
    const reaction = reactionToFailure(error, "mutation");
    if (reaction.type === "cancelled") {
      return;
    }
    options.report?.(error);
    switch (reaction.type) {
      case "notExecuted":
        toast.error(
          <TranslateEmbedded
            textKey={reaction.failure.type === "serviceNotReady" ? "unavailableService.notReady" : "unavailableService.unreachable"}
          />,
          { id: mutationFailedToastId },
        );
        return;
      case "unknownOutcome":
        if (!recordUnknownOutcome(undefined)) {
          showUnknownOutcomeToast(<TranslateEmbedded textKey="unknownOutcome.titleWithoutAction" />);
        }
        return;
      case "sessionExpired":
      case "permissionDenied":
      case "versionMismatch":
      case "errorPage":
        toast.error(<TranslateEmbedded textKey="formFeedback.saveFailed" />, { id: mutationFailedToastId });
        return;
      default:
        throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(reaction)}`);
    }
  };
}
