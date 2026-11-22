import { refetchQueriesExceptPrevented, useQueryClient } from "@neckar-hub/typescript-react-query";
import { useGetNow } from "@neckar-hub/typescript-react-utils/providers";
import { useCallback, useEffect, useState } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../../primitives/alert-dialog";
import { Button } from "../../primitives/button";
import { mutationFailedToastId, toast } from "../toast/toast";
import { clearUnknownOutcome, clearUnknownOutcomeOf, useUnknownOutcome } from "./unknownOutcomeStore";

export interface UnknownOutcomeAlertProps {
  /** Id of the form shell rendering the alert; only the shell that was submitting shows it. */
  shellId: string;
  onSendAgain: () => void;
  /** Disables "send again" while the shell's own submit is disabled or locked. */
  sendAgainDisabled?: boolean;
  /** Closes the dialog or leaves the form page. */
  onCloseForm: () => void;
}

/**
 * The outcome of the mutation is unknown (504 or broken connection): reloads the server state once, keeps the entered values.
 * Reads `NowProvider` on its first render, so a missing provider fails at once instead of when the outcome becomes unknown.
 */
export function UnknownOutcomeAlert(props: UnknownOutcomeAlertProps): React.ReactElement | null {
  const { shellId } = props;
  const getNow = useGetNow();
  const mutation = useUnknownOutcome(shellId);

  // A shell that leaves the page takes its pending decision along.
  useEffect(() => () => clearUnknownOutcomeOf(shellId), [shellId]);

  if (mutation === undefined) {
    return null;
  }
  return (
    <UnknownOutcomeDialog
      action={mutation.action}
      getNow={getNow}
      onSendAgain={props.onSendAgain}
      sendAgainDisabled={props.sendAgainDisabled ?? false}
      onCloseForm={props.onCloseForm}
    />
  );
}

function UnknownOutcomeDialog(props: {
  action: string | undefined;
  getNow: () => Date;
  onSendAgain: () => void;
  sendAgainDisabled: boolean;
  onCloseForm: () => void;
}): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const reloadServerState = useReloadServerState();
  const { getNow } = props;
  const [reloadedAt, setReloadedAt] = useState<Date | undefined>(undefined);

  const reload = useCallback(() => {
    // A failed refetch keeps the text before the reload: the dialog names no server state it did not read.
    reloadServerState().then(
      () => setReloadedAt(getNow()),
      () => undefined,
    );
  }, [reloadServerState, getNow]);

  useEffect(() => {
    reload();
  }, [reload]);

  const sendAgain = () => {
    clearUnknownOutcome();
    props.onSendAgain();
  };
  const closeForm = () => {
    clearUnknownOutcome();
    props.onCloseForm();
  };

  return (
    <AlertDialog open={true}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {props.action === undefined
              ? t("unknownOutcome.titleWithoutAction")
              : t({ key: "unknownOutcome.title", options: { action: props.action } })}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {reloadedAt === undefined
              ? t("unknownOutcome.text")
              : t({ key: "unknownOutcome.textAfterReload", options: { time: reloadedAt.toLocaleTimeString() } })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button variant="outline" onClick={reload}>
            {t("unknownOutcome.reloadState")}
          </Button>
          <Button variant="outline" onClick={closeForm}>
            {t("unknownOutcome.closeForm")}
          </Button>
          <Button disabled={props.sendAgainDisabled} onClick={sendAgain}>
            {t("unknownOutcome.sendAgain")}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

/**
 * Fetches the queries a successful mutation reloads again; their data stays on screen, so the form keeps its input.
 */
function useReloadServerState(): () => Promise<void> {
  const queryClient = useQueryClient();
  return useCallback(() => refetchQueriesExceptPrevented(queryClient, { throwOnError: true }), [queryClient]);
}

/**
 * Toast content for an unanswered action outside a form shell: the text of `UnknownOutcomeAlert` and the reload of the server state.
 */
function UnknownOutcomeToastContent(): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const reloadServerState = useReloadServerState();

  return (
    <div className="flex flex-col items-start gap-2">
      <span>{t("unknownOutcome.textOutsideForm")}</span>
      <Button size="sm" variant="outline" onClick={() => void reloadServerState().catch(() => undefined)}>
        {t("unknownOutcome.reloadState")}
      </Button>
    </div>
  );
}

/**
 * Shows the text and the reload of `UnknownOutcomeAlert` as a lasting toast, for an action outside a form shell or one whose shell is gone.
 */
export function showUnknownOutcomeToast(title: React.ReactNode): void {
  toast.error(title, { id: mutationFailedToastId, description: <UnknownOutcomeToastContent />, duration: Infinity });
}
