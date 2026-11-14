import { type ParseKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { ReactElement, ReactNode } from "react";
import { toast, withActionFailedToast } from "../../feedback";
import { TitleWithEntity } from "../../text/title-with-entity";
import { ConfirmationDialog } from "../confirmation-dialog";

export interface EntityUnarchiveDialogProps {
  /**
   * Translation key of the label in front of `display` in the heading.
   */
  title: ParseKeys;
  /**
   * The entity to unarchive, rendered in the heading.
   */
  display: ReactNode;
  description: ParseKeys;
  /**
   * Translation key of the confirm button; also names the action in the failure toast.
   */
  confirmLabel: ParseKeys;
  /**
   * Translation key of the success toast.
   */
  unarchivedMessage: ParseKeys;
  unarchive: () => Promise<unknown>;
  onOpenChange: (open: boolean) => void;
  /**
   * Called after `unarchive` resolved; a rejection keeps the dialog open and shows the failure toast instead.
   */
  onUnarchived?: () => void;
}

/**
 * Dialog to confirm reactivating (unarchiving) an entity. Always open; the caller mounts it to show it and unmounts it on
 * `onOpenChange(false)`.
 */
export function EntityUnarchiveDialog({
  title,
  display,
  description,
  confirmLabel,
  unarchivedMessage,
  unarchive,
  onOpenChange,
  onUnarchived,
}: EntityUnarchiveDialogProps): ReactElement {
  const { t } = useTranslation();

  const handleUnarchive = async () => {
    await withActionFailedToast(t(confirmLabel), unarchive());
    onOpenChange(false);
    toast.success(t(unarchivedMessage));
    onUnarchived?.();
  };

  return (
    <ConfirmationDialog
      description={t(description)}
      textKeys={{ confirm: t(confirmLabel) }}
      title={<TitleWithEntity label={t(title)}>{display}</TitleWithEntity>}
      onConfirm={handleUnarchive}
      onOpenChange={onOpenChange}
    />
  );
}
