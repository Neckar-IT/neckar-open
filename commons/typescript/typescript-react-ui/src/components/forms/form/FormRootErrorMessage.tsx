import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { useFormContext } from "react-hook-form";
import { FormErrorText } from "./FormErrorText";

/**
 * The rejection under `formState.errors.root`, which no form field owns, such as a server rule spanning several properties.
 * {@link FormCardFooter} renders it; a form without a card renders it once above its buttons.
 */
export function FormRootErrorMessage({ className }: { className?: string }): React.ReactElement | null {
  const { formState } = useFormContext();
  const message = formState.errors.root?.message;

  if (isNotDefined(message)) {
    return null;
  }

  return (
    <FormErrorText className={className} role="alert">
      {message}
    </FormErrorText>
  );
}
