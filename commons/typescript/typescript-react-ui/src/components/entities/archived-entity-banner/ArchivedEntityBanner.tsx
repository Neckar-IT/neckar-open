import { ArchiveState } from "@neckar-hub/typescript-utils/openapi-types";
import { ArchiveIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { type Action, ActionButton } from "../../actions";
import { Alert, AlertDescription, AlertTitle } from "../../primitives/alert";

interface ArchivedEntityBannerProps {
  archiveState: ArchiveState;
  /**
   * Unarchive action rendered as the reactivate button. Supplied by the caller
   * so the banner stays entity-agnostic and the button inherits the same label, icon,
   * and mutation wiring as the dropdown entry.
   */
  unarchiveAction: Action;
}

/**
 * Top-of-page banner shown on detail pages when the entity is archived. Returns `null`
 * for active entities — callers can render it unconditionally.
 *
 * Built on the shared `Alert` primitive with the neutral `muted` variant — archive is
 * not an alarm. The muted background plus muted icon and text signal "out of the active
 * flow" without screaming at the user.
 *
 * The banner text is entity-agnostic and lives in a single central translation key, so
 * new entity types inherit the same wording automatically.
 */
export function ArchivedEntityBanner({ archiveState, unarchiveAction }: ArchivedEntityBannerProps): React.ReactElement | null {
  const { t } = useEmbeddedTranslation();

  switch (archiveState) {
    case ArchiveState.Active:
      return null;
    case ArchiveState.Archived:
      return (
        <Alert variant="muted" className="flex items-start justify-between">
          <ArchiveIcon className="size-4" />
          <div>
            <AlertTitle>{t("archivedBanner.title")}</AlertTitle>
            <AlertDescription>{t("archivedBanner.description")}</AlertDescription>
          </div>
          <ActionButton action={unarchiveAction} variant="link" size="sm" />
        </Alert>
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: archiveState`);
  }
}
