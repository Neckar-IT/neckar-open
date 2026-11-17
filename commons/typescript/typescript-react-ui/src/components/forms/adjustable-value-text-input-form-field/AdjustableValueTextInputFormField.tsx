/**
 * Imports
 */

import { FormFieldBase, type FormFieldBaseProps } from "@neckar-hub/typescript-react-ui/forms";
import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { z, type ZodObject } from "zod";
import { AdjustableValueTextInput, type AdjustableValueTextInputProps } from "../../inputs/adjustable-value-text-input";

/**
 * Constants
 */

const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * Zod Schema for the Adjustable Value of type String
 */

export const adjustableValueTextSchema = z.discriminatedUnion("type", [
  z.object({
    type: z.literal(Adjusted),
    calculated: z.string(),
    value: z.string(),
  }),
  z.object({
    type: z.literal(Auto),
    value: z.string(),
  }),
  z.object({
    type: z.literal(Manual),
    value: z.string(),
  }),
]);

/**
 * Prop Types
 */

interface AdjustableValueTextInputFormFieldProps<T extends ZodObject>
  extends FormFieldBaseProps<T>, Omit<AdjustableValueTextInputProps, "value" | "onChange"> {}

/**
 * Form Field for Adjustable Text Values
 */

export function AdjustableValueTextInputFormField<T extends ZodObject>({
  type,
  textKeys,
  ...rest
}: AdjustableValueTextInputFormFieldProps<T>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <AdjustableValueTextInput {...field} isInvalid={fieldState.invalid} textKeys={textKeys} type={type} />
      )}
    />
  );
}
