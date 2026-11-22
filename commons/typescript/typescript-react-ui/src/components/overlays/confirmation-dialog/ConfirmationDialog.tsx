/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import { isPromiseLike } from "@neckar-hub/typescript-utils/promise-utils";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { ActionButton } from "../../actions/ActionButton";
import { withActionFailedToast } from "../../feedback";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../../primitives/alert-dialog";
import { usePendingAction } from "./usePendingAction";

/**
 * Button variants that make sense for a confirmation dialog's primary action. Excludes
 * `outline`, `ghost`, `link` — those read as secondary in a dialog footer and would
 * fail to signal the commit step.
 */
export type ConfirmationDialogVariant = "confirm" | "destructive" | "default" | "secondary";

/**
 * Prop Types
 */

export interface ConfirmationDialogTextKeys {
  title: TextKey;
  description: TextKey;
  cancel: TextKey;
  confirm: TextKey;
}

export interface ConfirmationDialogProps {
  /**
   * Dialog title. Takes precedence over [textKeys.title] — pass it for dynamic content such
   * as an interpolated entity name (`"Müller GmbH" wirklich löschen?`). When omitted, the
   * component falls back to [textKeys.title], then to the `confirmationDialog.title` default.
   */
  title?: React.ReactNode;
  /**
   * Dialog description. Takes precedence over [textKeys.description] — pass it for dynamic
   * content. When both are omitted, no description is rendered (mirrors `SaveDialog`).
   */
  description?: React.ReactNode;
  /**
   * Block content between the header and the footer, such as a list. It stays outside the description, which Radix
   * renders as a `<p>` and announces on open.
   */
  children?: React.ReactNode;
  /**
   * Runs the action. A rejected promise goes to `withActionFailedToast`, labelled with the confirm button's text.
   */
  onConfirm: () => void | Promise<void>;
  open?: boolean;
  onOpenChange: (open: boolean) => void;
  isPending?: boolean;
  textKeys?: Partial<ConfirmationDialogTextKeys>;
  /**
   * Variant of the confirm button. Defaults to `confirm` for non-destructive actions
   * (reactivate, publish, restore, …). Set to `destructive` when the action cannot be
   * undone — or, preferably, reach for {@link DestructiveConfirmationDialog}, which is
   * a convenience wrapper that pins this to `destructive` and documents the intent at
   * the call site.
   */
  variant?: ConfirmationDialogVariant;
}

/**
 * Dialog to confirm a user action. The confirm button's variant is switchable via
 * [variant] — default is the non-destructive `confirm`. For destructive flows prefer
 * {@link DestructiveConfirmationDialog}, the convenience wrapper that pins the variant
 * to `destructive` and makes the severity obvious at the call site.
 *
 * @prop open - Whether the dialog is open or not (if omitted, the dialog is always open and you need to use conditional rendering to show/hide it)
 */

export function ConfirmationDialog({
  title,
  description,
  children,
  onConfirm,
  open = true,
  onOpenChange,
  isPending: externalPending,
  textKeys,
  variant = "confirm",
}: ConfirmationDialogProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  const resolvedTitle: React.ReactNode = title ?? t(textKeys?.title ?? "confirmationDialog.title");
  const resolvedDescription: React.ReactNode = description ?? (textKeys?.description !== undefined ? t(textKeys.description) : undefined);

  const cancelKey: TextKey = textKeys?.cancel ?? "confirmationDialog.cancel";
  const confirmKey: TextKey = textKeys?.confirm ?? "confirmationDialog.confirm";

  const confirmLabel: string = t(confirmKey);
  const confirm = (): void | Promise<void> => {
    const result = onConfirm();
    // `withActionFailedToast` decides the feedback for the rejection, so it ends here instead of escaping the click handler.
    return isPromiseLike<void>(result) ? withActionFailedToast(confirmLabel, result).catch(() => undefined) : result;
  };
  const { run, isPending } = usePendingAction(confirm, { externalPending });

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{resolvedTitle}</AlertDialogTitle>
          {resolvedDescription !== undefined ? <AlertDialogDescription>{resolvedDescription}</AlertDialogDescription> : null}
        </AlertDialogHeader>

        {children}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isPending}>{t(cancelKey)}</AlertDialogCancel>
          {/* An ActionButton, so an `ActionLockProvider` lock (e.g. offline) disables the confirmation too. */}
          <ActionButton
            action={{ icon: undefined, label: confirmLabel, loading: isPending, onTrigger: () => void run() }}
            variant={variant}
          />
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
