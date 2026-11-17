/**
 * Imports
 */

import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { useSupportContactEmail } from "../../SupportContactContext";
import { buildSupportMailtoUrl } from "./supportMailtoUrl";

/**
 * Prop Types
 */

interface ContactSupportButtonProps {
  /** Text from [buildErrorInfoText]. */
  errorInfo: string;
}

/**
 * Link that opens the mail client with a support mail carrying the error information.
 */

export function ContactSupportButton({ errorInfo }: ContactSupportButtonProps) {
  const { t } = useEmbeddedTranslation();
  const contactEmail = useSupportContactEmail();
  const label = t("contactSupportButton.label");

  const mailtoUrl = buildSupportMailtoUrl({
    contactEmail,
    subject: t({ key: "contactSupportButton.mailSubject", options: { host: window.location.host } }),
    bodyIntro: t("contactSupportButton.mailBodyIntro"),
    errorInfo,
    truncationHint: t({ key: "contactSupportButton.truncationHint", options: { contactSupportLabel: label } }),
  });

  return (
    <Button asChild={true} size="sm" variant="outline">
      <a href={mailtoUrl}>{label}</a>
    </Button>
  );
}
