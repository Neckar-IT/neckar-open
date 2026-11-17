/**
 * Imports
 */

import type { ZodObject } from "zod";
import { PasswordInput } from "../../inputs/password-input";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Password Input Form Field
 */

interface PasswordInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  autoComplete?: "current-password" | "new-password";
}

/**
 * Password input with show/hide toggle, bound to a react-hook-form field.
 */
export function PasswordInputFormField<T extends ZodObject>({ autoComplete, ...rest }: PasswordInputFormFieldProps<T>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => <PasswordInput autoComplete={autoComplete} isInvalid={fieldState.invalid} {...field} />}
    />
  );
}
