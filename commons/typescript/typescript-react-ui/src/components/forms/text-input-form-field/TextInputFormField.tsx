/**
 * Imports
 */

import type { ZodObject } from "zod";
import { TextInput } from "../../inputs/text-input";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Type Declarations
 */

export interface TextInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  /**
   * The HTML input type
   */
  type?: "text" | "email" | "url";
  /**
   * The placeholder that is shown when the input is empty
   */
  placeholder?: string | undefined;
  maxLength?: number;
  /**
   * The autocomplete attribute for the input element (https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete)
   */
  autoComplete?: string | undefined;
}

/**
 * React-hook-form field wrapper around TextInput for single-line text, email, or url inputs.
 */

export function TextInputFormField<T extends ZodObject>({
  type,
  placeholder,
  maxLength,
  autoComplete,
  ...rest
}: TextInputFormFieldProps<T>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <TextInput
          {...field}
          type={type}
          placeholder={placeholder}
          maxLength={maxLength}
          autoComplete={autoComplete}
          isInvalid={fieldState.invalid}
        />
      )}
    />
  );
}
