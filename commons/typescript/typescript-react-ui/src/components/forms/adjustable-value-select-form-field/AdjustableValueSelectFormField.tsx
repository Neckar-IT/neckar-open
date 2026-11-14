/**
 * Imports
 */

import { FormFieldBase, type FormFieldBaseProps } from "@neckar-hub/typescript-react-ui/forms";
import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { z, type ZodObject, type ZodType } from "zod";
import { AdjustableValueSelect, type AdjustableValueSelectProps } from "../../inputs/adjustable-value-select";

/**
 * Constants
 */

const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * Builds the zod schema for an adjustable value whose effective value is described by `valueSchema`.
 *
 * The whole field is nullable — the inherited state with nothing to inherit. An override may be empty
 * (`value: null`) while the user picks one; the inherited `Auto` value and the `Adjusted` calculated
 * counterpart are always present.
 */
export function adjustableValueSelectSchema<S extends ZodType>(valueSchema: S) {
  return z
    .discriminatedUnion("type", [
      z.object({ type: z.literal(Auto), value: valueSchema }),
      z.object({ type: z.literal(Adjusted), calculated: valueSchema, value: valueSchema.nullable() }),
      z.object({ type: z.literal(Manual), value: valueSchema.nullable() }),
    ])
    .nullable();
}

/**
 * Prop Types
 */

export interface AdjustableValueSelectFormFieldProps<T extends ZodObject, U>
  extends FormFieldBaseProps<T>, Omit<AdjustableValueSelectProps<U>, "value" | "onChange" | "isInvalid"> {}

/**
 * Form Field for Adjustable Select Values
 */

export function AdjustableValueSelectFormField<T extends ZodObject, U>({
  options,
  itemRenderer,
  textKeys,
  placeholder,
  ...rest
}: AdjustableValueSelectFormFieldProps<T, U>): React.ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => (
        <AdjustableValueSelect
          {...field}
          isInvalid={fieldState.invalid}
          options={options}
          itemRenderer={itemRenderer}
          textKeys={textKeys}
          placeholder={placeholder}
        />
      )}
    />
  );
}
