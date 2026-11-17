import { ConfirmationDialog, type ConfirmationDialogProps } from "./ConfirmationDialog";

/**
 * Convenience wrapper around {@link ConfirmationDialog} that defaults the confirm button
 * variant to `destructive` and shifts two embedded fallbacks from the neutral namespace to
 * the destructive one: the confirm label from `confirmationDialog.confirm` ("Bestätigen") to
 * `destructiveConfirmationDialog.confirm` ("Löschen"), and the description fallback to
 * `destructiveConfirmationDialog.description` ("Das Löschen kann nicht rückgängig gemacht
 * werden."). Use this for flows that cannot be undone (delete, archive, force-reset) — the
 * distinct component name makes the severity obvious at the call site and prevents destructive
 * intent from hiding inside a neutral `<ConfirmationDialog>`.
 *
 * Equivalent to writing `<ConfirmationDialog variant="destructive" textKeys={…}>`. Callers
 * supplying their own `textKeys.confirm` / `textKeys.description`, or a direct `description`
 * prop, keep full control; a default only kicks in when the caller omits both the key and the
 * prop.
 */
export function DestructiveConfirmationDialog({ variant = "destructive", textKeys, ...rest }: ConfirmationDialogProps): React.ReactElement {
  const textKeysWithDestructiveDefaults = {
    ...textKeys,
    confirm: textKeys?.confirm ?? "destructiveConfirmationDialog.confirm",
    description: textKeys?.description ?? "destructiveConfirmationDialog.description",
  };
  return <ConfirmationDialog variant={variant} textKeys={textKeysWithDestructiveDefaults} {...rest} />;
}
