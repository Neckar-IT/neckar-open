/**
 * Imports
 */

import { type ZodObject } from "zod";
import { Textarea } from "../../inputs/textarea";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Type Declarations
 */

export interface TextareaFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  maxLength?: number;
  rows?: number;
  resizable?: boolean;
  placeholder?: string;
}

/**
 * React-hook-form field wrapper around Textarea for multi-line text inputs.
 */

export function TextareaFormField<T extends ZodObject>({ maxLength, rows, resizable, placeholder, ...rest }: TextareaFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <Textarea
          {...field}
          isInvalid={fieldState.invalid}
          maxLength={maxLength}
          rows={rows}
          resizable={resizable}
          placeholder={placeholder}
        />
      )}
    />
  );
}
