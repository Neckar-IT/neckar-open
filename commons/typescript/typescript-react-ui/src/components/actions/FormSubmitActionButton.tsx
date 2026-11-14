import { useId, useLayoutEffect, useRef } from "react";
import { useFormState } from "react-hook-form";
import { UnknownOutcomeAlert } from "../feedback/unknown-outcome";
import { useSubmittingShell } from "../feedback/unknown-outcome/unknownOutcomeStore";
import { ActionButton, type ActionButtonProps } from "./ActionButton";
import { useActionState } from "./useActionState";

export interface FormSubmitActionButtonProps extends ActionButtonProps {}

/**
 * A submit button that executes an Action directly on click (not via form submission).
 *
 * Wraps {@link ActionButton} and additionally registers an Enter keydown listener
 * on the parent `<form>` element so that pressing Enter in input fields triggers
 * the action — matching native form behavior.
 * Enter in `<textarea>` fields is ignored (produces a newline instead).
 *
 * Must be rendered inside a react-hook-form {@link FormProvider} — derives
 * its loading state from the form's `isSubmitting` state by default
 * (mirrors how {@link FormCancelButton} uses `isSubmitting` for `disabled`).
 * Callers can override with an explicit `loading` prop or via `action.loading`.
 * Enter does nothing while the button is disabled, locked or loading, like a click.
 * Shows `UnknownOutcomeAlert` when the outcome of the form submit is unknown; "close form" leaves the form page.
 */
export function FormSubmitActionButton({ action, loading, disabled, ...rest }: FormSubmitActionButtonProps): React.ReactElement | null {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const shellId = useId();
  const { isSubmitting } = useFormState();

  const hidden = action.hidden ?? false;
  // A hidden button renders no UnknownOutcomeAlert, so it claims no unknown outcome either.
  useSubmittingShell(shellId, isSubmitting && !hidden);

  // Precedence: explicit `loading` prop → `action.loading` → `isSubmitting`.
  // This keeps the common case (Upsert-Action via RHF handleSubmit) spinner-
  // enabled automatically, while still allowing overrides.
  const effectiveLoading = loading ?? action.loading ?? isSubmitting;
  const { blocked } = useActionState(action, { disabled, loading: effectiveLoading });

  useLayoutEffect(() => {
    if (hidden) {
      return;
    }

    const form = buttonRef.current?.closest("form");
    if (!form) {
      throw new Error("FormSubmitActionButton must be placed inside a <Form> (or <form>) element");
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== "Enter") {
        return;
      }

      if (e.target instanceof HTMLTextAreaElement) {
        return;
      }

      if (e.metaKey || e.ctrlKey || e.altKey || e.shiftKey) {
        return;
      }

      e.preventDefault();
      if (blocked) {
        return;
      }
      action.onTrigger();
    };

    form.addEventListener("keydown", handleKeyDown);
    return () => form.removeEventListener("keydown", handleKeyDown);
  }, [action, hidden, blocked]);

  return (
    <>
      <ActionButton ref={buttonRef} action={action} type="submit" loading={effectiveLoading} disabled={disabled} {...rest} />
      {!hidden && (
        <UnknownOutcomeAlert shellId={shellId} onSendAgain={action.onTrigger} sendAgainDisabled={blocked} onCloseForm={leaveFormPage} />
      )}
    </>
  );
}

/**
 * Steps back from the form page; a page opened as the first history entry goes to the application's entry point instead.
 */
function leaveFormPage(): void {
  if (window.history.length > 1) {
    window.history.back();
  } else {
    window.location.assign(import.meta.env.BASE_URL);
  }
}
