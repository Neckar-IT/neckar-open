import { type ParseKeys, useTranslation } from "@neckar-hub/typescript-react-i18n";
import type { ReactElement, ReactNode } from "react";
import { CancelButton } from "../../actions/cancel-button";
import { Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "../../primitives/dialog";

export interface EntitySelectDialogProps {
  /**
   * Translation key of the heading, resolved against the app's translations.
   */
  title: ParseKeys;
  onOpenChange: (open: boolean) => void;
  /**
   * The select table of the entity.
   */
  children: ReactNode;
}

/**
 * The frame of an entity's pick dialog: heading, the select table as body and a cancel button. Always open; the caller
 * mounts it to show it and unmounts it on `onOpenChange(false)`.
 */
export function EntitySelectDialog({ title, onOpenChange, children }: EntitySelectDialogProps): ReactElement {
  const { t } = useTranslation();

  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent className="min-w-4xl">
        <DialogHeader>
          <DialogTitle>{t(title)}</DialogTitle>
        </DialogHeader>
        <DialogBody>{children}</DialogBody>

        <DialogFooter>
          <CancelButton onClick={() => onOpenChange(false)} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
