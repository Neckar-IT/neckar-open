/**
 * Imports
 */

import { cn } from "cn";
import { type ZodObject } from "zod";
import { DurationInput } from "../../inputs/duration-input";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface DurationInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  step?: number;
  min?: number;
  max?: number;
}

/**
 * Duration Input Form Field Component (value is in milliseconds!)
 */

export function DurationInputFormField<T extends ZodObject>({ step, min, max, ...rest }: DurationInputFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <DurationInput
          {...field}
          step={step}
          min={min}
          max={max}
          className={cn({ "border-destructive focus-visible:ring-destructive/30": fieldState.invalid })}
        />
      )}
    />
  );
}
