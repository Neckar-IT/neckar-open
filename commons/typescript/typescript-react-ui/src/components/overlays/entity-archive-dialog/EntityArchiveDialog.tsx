import { type ParseKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { ReactElement, ReactNode } from "react";
import { type ActiveUsageCounts, toast, warnAboutActiveUsage } from "../../feedback";
import { TitleWithEntity } from "../../text/title-with-entity";
import { DestructiveConfirmationDialog } from "../confirmation-dialog";

/**
 * The answer of an archive request; `good` carries the records that still depend on the archived one.
 */
export type ArchiveOutcome = { readonly type: "good"; readonly usedBy: ActiveUsageCounts } | { readonly type: `bad/${string}` };

/**
 * Translation keys of the toast after each archive answer.
 */
export interface EntityArchiveMessages {
  /**
   * Success toast of a `good` answer.
   */
  archived: ParseKeys;
  /**
   * Error toast of a refusal: every `bad/*` answer other than `bad/AlreadyArchived` and `bad/NotFound`.
   */
  refused?: ParseKeys;
  /**
   * Error toast of `bad/AlreadyArchived` and `bad/NotFound`; also of a refusal without `refused`.
   */
  failed: ParseKeys;
}

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
   * Translation key of the confirm button; also names the action in the action-failed toast.
   */
  confirmLabel: ParseKeys;
  messages: EntityArchiveMessages;
  archive: () => Promise<ArchiveOutcome>;
  onOpenChange: (open: boolean) => void;
  /**
   * Called after a `good` answer; a rejected `archive` keeps the dialog open and shows the action-failed toast instead.
   */
  onArchived?: () => void;
}

/**
 * Dialog to confirm archiving an entity. Always open; the caller mounts it to show it and unmounts it on
 * `onOpenChange(false)`. Every answer closes the dialog and shows the toast `messages` names for it; a `good` answer with active
 * dependent records adds the `actionFeedback.activeUsage` warning toast.
 */
export function EntityArchiveDialog({
  title,
  display,
  description,
  usage,
  confirmLabel,
  messages,
  archive,
  onOpenChange,
  onArchived,
}: EntityArchiveDialogProps): ReactElement {
  const { t } = useTranslation();

  const handleArchive = async () => {
    const outcome = await archive();
    onOpenChange(false);

    switch (outcome.type) {
      case "good":
        toast.success(t(messages.archived));
        warnAboutActiveUsage(outcome.usedBy);
        onArchived?.();
        return;
      case "bad/AlreadyArchived":
      case "bad/NotFound":
        toast.error(t(messages.failed));
        return;
      default:
        toast.error(t(messages.refused ?? messages.failed));
    }
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
