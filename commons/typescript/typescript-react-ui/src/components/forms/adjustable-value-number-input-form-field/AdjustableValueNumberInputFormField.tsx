/**
 * Imports
 */

import { FormFieldBase, type FormFieldBaseProps } from "@neckar-hub/typescript-react-ui/forms";
import {
  AdjustableValueAdjustedDoubleWithTypeType,
  AdjustableValueAutoDoubleWithTypeType,
  AdjustableValueManualDoubleWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { z, type ZodObject } from "zod";
import { AdjustableValueNumberInput, type AdjustableValueNumberInputProps } from "../../inputs/adjustable-value-number-input";

/**
 * Constants
 */

const { Adjusted } = AdjustableValueAdjustedDoubleWithTypeType;
const { Auto } = AdjustableValueAutoDoubleWithTypeType;
const { Manual } = AdjustableValueManualDoubleWithTypeType;

/**
 * Zod Schema for the Adjustable Value of type Number
 *
 * The constraint on the amount is passed in, because the wrapper says where a number came from and
 * never what it may be: a count is a non-negative integer whether it was calculated or typed. It
 * holds for the calculated side too — [adjustableValueNumber] applies it to both, so a schema
 * cannot end up admitting through one variant what it rejects through another.
 */

export function adjustableValueNumber(value: z.ZodNumber) {
  return z.discriminatedUnion("type", [
    z.object({
      type: z.literal(Adjusted),
      calculated: value,
      value: value,
    }),
    z.object({
      type: z.literal(Auto),
      value: value,
    }),
    z.object({
      type: z.literal(Manual),
      value: value,
    }),
  ]);
}

/** Any number at all — for a quantity nothing constrains. */
export const adjustableValueNumberSchema = adjustableValueNumber(z.number());

/**
 * Prop Types
 */

export interface AdjustableValueNumberInputFormFieldProps<T extends ZodObject>
  extends FormFieldBaseProps<T>, Omit<AdjustableValueNumberInputProps, "value" | "onChange" | "isInvalid"> {}

/**
 * Form Field for Adjustable Number Values
 */

export function AdjustableValueNumberInputFormField<T extends ZodObject>({
  className,
  disabled,
  ...rest
}: AdjustableValueNumberInputFormFieldProps<T>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      className={className}
      disabled={disabled}
      render={({ field, fieldState }) => <AdjustableValueNumberInput {...rest} {...field} isInvalid={fieldState.invalid} />}
    />
  );
}
