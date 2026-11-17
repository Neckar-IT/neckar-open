import { useFormState } from "react-hook-form";
import { CancelButton, type CancelButtonProps } from "../../actions/cancel-button";

interface FormCancelButtonProps extends CancelButtonProps {}

/**
 * Form Cancel Button
 *
 * Use this button to cancel a form submission. It will automatically be disabled while the form is submitting to prevent multiple submissions.
 * Must be used within a Form Context (e.g. within a Form component) to work properly.
 */
export function FormCancelButton({ disabled, ...rest }: FormCancelButtonProps): React.ReactElement {
  const { isSubmitting } = useFormState();

  return <CancelButton disabled={isSubmitting || disabled} {...rest} />;
}
