import { ArchiveState, type ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { ConfirmationStatusBadge } from "../../draft-confirmed/ConfirmationStatusBadge";
import { ArchivedBadge } from "../archived-badge/ArchivedBadge";
import type { StatusBadgeVariant } from "../status-badge/StatusBadge";

interface EntityLike {
  archiveState: ArchiveState;
  confirmationStatus?: ConfirmationStatus;
}

interface EntityStatusBadgeProps<T extends EntityLike = EntityLike> {
  entity: T | null;
  variant?: StatusBadgeVariant;
  className?: string;
}

/**
 * Combined status badge for an entity's archive state and confirmation status.
 */
export function EntityStatusBadge<T extends EntityLike>({
  entity,
  variant = "icon",
  className,
}: EntityStatusBadgeProps<T>): React.ReactElement | null {
  if (entity === null) {
    return null;
  }

  switch (entity.archiveState) {
    case ArchiveState.Archived:
      return <ArchivedBadge archiveState={entity.archiveState} variant={variant} className={className} />;
    case ArchiveState.Active:
      if (entity.confirmationStatus === undefined) {
        return null;
      }
      return <ConfirmationStatusBadge status={entity.confirmationStatus} variant={variant} className={className} />;
    default:
      throw new Error(`Unexpected value in Switch Statement: entity.archiveState`);
  }
}
