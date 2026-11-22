import { ArchiveState } from "@neckar-hub/typescript-utils/openapi-types";
import { ArchiveIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { StatusBadge, type StatusBadgeVariant } from "../status-badge/StatusBadge";

interface ArchivedBadgeProps {
  archiveState: ArchiveState;
  variant?: StatusBadgeVariant;
  className?: string;
}

/**
 * Badge marking an entity as archived; renders nothing when the entity is active.
 */
export function ArchivedBadge({ archiveState, variant, className }: ArchivedBadgeProps): React.ReactElement | null {
  const { t } = useEmbeddedTranslation();

  switch (archiveState) {
    case ArchiveState.Active:
      return null;
    case ArchiveState.Archived:
      return <StatusBadge tone="muted" icon={<ArchiveIcon />} label={t("archiveState.Archived")} variant={variant} className={className} />;
    default:
      throw new Error(`Unexpected value in Switch Statement: archiveState`);
  }
}
