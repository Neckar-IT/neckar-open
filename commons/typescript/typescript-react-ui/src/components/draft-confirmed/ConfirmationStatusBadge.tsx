import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { ClipboardCheckIcon, ClipboardPenIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { StatusBadge, type StatusBadgeVariant } from "../entities/status-badge/StatusBadge";

interface ConfirmationStatusBadgeProps {
  status: ConfirmationStatus;
  variant?: StatusBadgeVariant;
  className?: string;
}

/**
 * Badge showing an entity's draft/confirmed confirmation status.
 */
export function ConfirmationStatusBadge({ status, variant, className }: ConfirmationStatusBadgeProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  switch (status) {
    case ConfirmationStatus.Draft:
      return (
        <StatusBadge
          tone="muted"
          icon={<ClipboardPenIcon />}
          label={t(`confirmationStatus.${status}`)}
          variant={variant}
          className={cn("border-draft/60 text-draft", className)}
        />
      );
    case ConfirmationStatus.Confirmed:
      return (
        <StatusBadge
          tone="success"
          icon={<ClipboardCheckIcon />}
          label={t(`confirmationStatus.${status}`)}
          variant={variant}
          className={cn("border-confirmed/60 text-confirmed", className)}
        />
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: status`);
  }
}
