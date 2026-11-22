import { useFormState } from "react-hook-form";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { LockableButton } from "../../actions/LockableButton";
import type { ButtonProps } from "../../primitives/button";

interface FormSubmitButtonProps extends Omit<ButtonProps, "type" | "loading"> {}

/**
 * A disabled submit button, e.g. under an `ActionLockProvider` lock, also stops the implicit submission by Enter in an input.
 */
export function FormSubmitButton({ children, ...rest }: FormSubmitButtonProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const { isValid, isSubmitting } = useFormState();

  return (
    <LockableButton type="submit" loading={isSubmitting} aria-invalid={!isValid} {...rest}>
      {children ?? t("submitButton.label")}
    </LockableButton>
  );
}
