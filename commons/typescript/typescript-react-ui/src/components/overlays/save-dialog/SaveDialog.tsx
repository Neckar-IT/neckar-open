/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { PropsWithChildren, ReactElement } from "react";
import { useCallback, useEffect } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { useActionLock } from "../../actions/ActionLockProvider";
import { LockableButton } from "../../actions/LockableButton";
import { Button } from "../../primitives/button";
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../../primitives/dialog";

/**
 * Type Declarations
 */

export interface SaveDialogTextKeys {
  /**
   * Heading of the dialog. Mandatory, because it is often the only place that names what the user is
   * doing — a generic default heading would leave that unsaid without anyone noticing.
   */
  title: TextKey;
  /**
   * Text below the heading. When omitted, no description is rendered.
   */
  description?: TextKey;
  cancel?: TextKey;
  submit?: TextKey;
}

export type SaveDialogProps = PropsWithChildren<
  Pick<React.ComponentProps<typeof DialogContent>, "size"> & {
    open?: boolean;
    onOpenChange: (open: boolean) => void;
    onSubmit: () => void;
    /**
     * Fires only when the user clicks the cancel button.
     */
    onCancel?: (() => void) | undefined;
    /**
     * Fires once whenever the dialog closes: cancel button, Escape, overlay click or close button.
     */
    onClose?: (() => void) | undefined;
    isPending?: boolean | undefined;
    disabledSubmission?: boolean | undefined;
    textKeys: SaveDialogTextKeys;
  }
>;

/**
 * Base Dialog with preconfigured Header and Footer
 *
 * @prop open - Whether the dialog is open (true by default, so you can either control it by passing in the prop or just render it)
 */

export function SaveDialog(props: SaveDialogProps): ReactElement | null {
  const { t } = useEmbeddedTranslation();
  const { title, description, cancel = "simpleDialog.cancel", submit = "simpleDialog.submit" } = props.textKeys;

  const open = props.open ?? true;
  const isLocked = useActionLock() !== undefined;
  // Enter submits only when a click on the submit button would.
  const submitBlocked = (props.disabledSubmission ?? false) || isLocked || (props.isPending ?? false);

  // Radix points aria-describedby at the DialogDescription it renders; without a description that
  // reference dangles, and only an explicit undefined removes it.
  const describedByProps = description === undefined ? { "aria-describedby": undefined } : {};

  const handleSubmit = useCallback(() => {
    props.onSubmit();
  }, [props]);

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen && props.onClose !== undefined) props.onClose();
    props.onOpenChange(nextOpen);
  };

  const handleCancel = () => {
    if (props.onCancel !== undefined) props.onCancel();
    handleOpenChange(false);
  };

  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter") {
        event.preventDefault(); // prevent any form submission if needed
        if (submitBlocked) return;
        handleSubmit();
      }
    };

    window.document.addEventListener("keydown", handleKeyDown);
    return () => {
      window.document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open, handleSubmit, submitBlocked]);

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent {...describedByProps} size={props.size}>
        <DialogHeader>
          <DialogTitle>{t(title)}</DialogTitle>
          {description !== undefined ? <DialogDescription>{t(description)}</DialogDescription> : null}
        </DialogHeader>

        <DialogBody>{props.children}</DialogBody>

        <DialogFooter>
          <Button disabled={props.isPending} variant="outline" onClick={handleCancel}>
            {t(cancel)}
          </Button>
          <LockableButton disabled={props.disabledSubmission} loading={props.isPending} onClick={handleSubmit}>
            {t(submit)}
          </LockableButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
