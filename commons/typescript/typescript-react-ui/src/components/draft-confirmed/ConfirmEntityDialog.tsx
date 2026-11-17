import { resetQueriesExceptPrevented, useQueryClient } from "@neckar-hub/typescript-react-query";
import { AlertCircleIcon } from "lucide-react";
import { useState } from "react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { CancelButton } from "../actions/cancel-button";
import { toast } from "../feedback";
import { Alert, AlertDescription, AlertTitle } from "../primitives/alert";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../primitives/alert-dialog";
import { ConfirmButton } from "./ConfirmButton";
import type { FieldValidationError } from "./validation";
import { ValidationErrorsList } from "./ValidationErrorsList";

export type ConfirmEntityResponse =
  | { type: "good" }
  | { type: "bad/ValidationFailure"; validationErrors?: FieldValidationError[] }
  | { type: "bad/NotFound" }
  | { type: "bad/VersionConflict" };

interface ConfirmEntityDialogProps {
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<ConfirmEntityResponse>;
  isPending: boolean;
  messages: {
    confirmed: string;
    validationFailed: string;
    notFoundDetails: string;
    /** Shown when the stored entity is on another version than the one the dialog confirms; an embedded text stands in when absent. */
    versionConflict?: string;
  };
}

/**
 * Confirmation dialog for confirming a draft entity.
 *
 * A `bad/VersionConflict` reloads every query, so the details show the stored version.
 */
export function ConfirmEntityDialog({ onOpenChange, onConfirm, isPending, messages }: ConfirmEntityDialogProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);
  const [validationErrors, setValidationErrors] = useState<FieldValidationError[]>([]);
  const queryClient = useQueryClient();

  const handleConfirm = async () => {
    const response = await onConfirm();

    switch (response.type) {
      case "good":
        toast.success(messages.confirmed);
        onOpenChange(false);
        break;
      case "bad/ValidationFailure":
        setErrorMessage(messages.validationFailed);
        setValidationErrors(response.validationErrors ?? []);
        break;
      case "bad/NotFound":
        toast.error(messages.notFoundDetails);
        onOpenChange(false);
        break;
      case "bad/VersionConflict":
        toast.error(messages.versionConflict ?? t("confirmation.versionConflict"));
        void resetQueriesExceptPrevented(queryClient);
        onOpenChange(false);
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: response.type`);
    }
  };

  return (
    <AlertDialog open={true} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("confirmation.title")}</AlertDialogTitle>
          <AlertDialogDescription>
            <span className="font-medium">{t("confirmation.description")}</span>
            <br />
            {t("confirmation.descriptionNote")}
          </AlertDialogDescription>
        </AlertDialogHeader>

        {errorMessage ? (
          <Alert variant="destructive">
            <AlertCircleIcon className="size-5" />
            <AlertTitle>{t("validation.errorOccurred")}</AlertTitle>
            <AlertDescription>
              {errorMessage}
              {validationErrors.length > 0 ? <ValidationErrorsList errors={validationErrors} /> : null}
            </AlertDescription>
          </Alert>
        ) : null}

        <AlertDialogFooter>
          <CancelButton onClick={() => onOpenChange(false)} disabled={isPending} />
          <ConfirmButton onClick={handleConfirm} loading={isPending} />
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
