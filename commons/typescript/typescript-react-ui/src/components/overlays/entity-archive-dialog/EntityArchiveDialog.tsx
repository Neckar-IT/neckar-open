import { type ParseKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { ReactElement, ReactNode } from "react";
import { type ActiveUsageCounts, toast, warnAboutActiveUsage, withActionFailedToast } from "../../feedback";
import { TitleWithEntity } from "../../text/title-with-entity";
import { DestructiveConfirmationDialog } from "../confirmation-dialog";

/**
 * The answer of an archive request; `good` carries the records that still depend on the archived one.
 */
export type ArchiveOutcome = { readonly type: "good"; readonly usedBy: ActiveUsageCounts } | { readonly type: `bad/${string}` };

export interface EntityArchiveDialogProps {
  /**
   * Translation key of the label in front of `display` in the heading.
   */
  title: ParseKeys;
  /**
   * The entity to archive, rendered in the heading.
   */
  display: ReactNode;
  description: ParseKeys;
  /**
   * Rendered below the translated description, outside the description element; typically the records that depend on the entity.
   */
  usage?: ReactNode;
  /**
   * Translation key of the confirm button; also names the action in the failure toast.
   */
  confirmLabel: ParseKeys;
  /**
   * Translation key of the success toast.
   */
  archivedMessage: ParseKeys;
  archive: () => Promise<ArchiveOutcome>;
  onOpenChange: (open: boolean) => void;
  /**
   * Called after `archive` resolved; a rejection keeps the dialog open and shows the failure toast instead.
   */
  onArchived?: () => void;
}

/**
 * Dialog to confirm archiving an entity. Always open; the caller mounts it to show it and unmounts it on
 * `onOpenChange(false)`. A `good` answer with active dependent records adds the `actionFeedback.activeUsage` warning toast.
 */
export function EntityArchiveDialog({
  title,
  display,
  description,
  usage,
  confirmLabel,
  archivedMessage,
  archive,
  onOpenChange,
  onArchived,
}: EntityArchiveDialogProps): ReactElement {
  const { t } = useTranslation();

  const handleArchive = async () => {
    const outcome = await withActionFailedToast(t(confirmLabel), archive());
    onOpenChange(false);
    toast.success(t(archivedMessage));
    if (outcome.type === "good") {
      warnAboutActiveUsage(outcome.usedBy);
    }
    onArchived?.();
  };

  return (
    <DestructiveConfirmationDialog
      description={t(description)}
      textKeys={{ confirm: t(confirmLabel) }}
      title={<TitleWithEntity label={t(title)}>{display}</TitleWithEntity>}
      onConfirm={handleArchive}
      onOpenChange={onOpenChange}
    >
      {usage}
    </DestructiveConfirmationDialog>
  );
}
