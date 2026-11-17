/**
 * Imports
 */

import { CopyTextToClipboardButton } from "@neckar-hub/typescript-react-ui/actions";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * Prop Types
 */

interface CopyErrorInfoButtonProps {
  /** Text from [buildErrorInfoText]. */
  errorInfo: string;
}

/**
 * Button that copies the error information to the clipboard.
 */

export function CopyErrorInfoButton({ errorInfo }: CopyErrorInfoButtonProps) {
  const { t } = useEmbeddedTranslation();

  return <CopyTextToClipboardButton size="sm" text={errorInfo} title={t("copyErrorInfoButton.label")} variant="outline" />;
}
