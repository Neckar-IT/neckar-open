import { Alert, AlertDescription } from "@neckar-hub/typescript-react-ui/feedback";
import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { InfoIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";

interface ReconfirmAfterSaveWarningProps {
  confirmationStatus: ConfirmationStatus;
}

/**
 * Displays an info alert warning that all data must be re-confirmed after saving.
 * Only renders when the entity is currently Confirmed.
 */
export function ReconfirmAfterSaveWarning({ confirmationStatus }: ReconfirmAfterSaveWarningProps): React.ReactElement | null {
  const { t } = useEmbeddedTranslation();

  if (confirmationStatus !== ConfirmationStatus.Confirmed) {
    return null;
  }

  return (
    <Alert variant="info" className="py-3">
      <InfoIcon className="size-4" />
      <AlertDescription>{t("confirmation.reconfirmAfterSave")}</AlertDescription>
    </Alert>
  );
}
