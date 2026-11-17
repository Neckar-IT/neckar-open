/**
 * Imports
 */

import type { ZodObject } from "zod";
import { FormFieldBase, type FormFieldBaseProps } from "../../forms/form";
import { TimeInput } from "./Input";

/**
 * Type Declarations
 */

export interface TimeInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  includeSeconds?: boolean;
}

/**
 * React-hook-form field wrapper around TimeInput for HH:MM or HH:MM:SS time values.
 */

export function TimeInputFormField<T extends ZodObject>({ includeSeconds, ...rest }: TimeInputFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState, formState }) => (
        <TimeInput
          {...field}
          includeSeconds={includeSeconds}
          isInvalid={fieldState.invalid}
          disabled={rest.disabled || formState.isSubmitting}
        />
      )}
    />
  );
}
