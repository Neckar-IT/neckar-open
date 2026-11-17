/**
 * Components to display and input a percentage that travels as PERCENTAGE POINTS.
 *
 * `40.0` means 40 %, which is the shape of the Kotlin value class `Percentage` and therefore the
 * shape on the wire — no conversion happens anywhere. Not to be confused with {@link VatRateOutput},
 * whose value is a fraction (`0.19` for 19 %) and which therefore converts.
 */

import type { ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "%";

/**
 * Displays a percentage given in percentage points (`40` renders as "40 %").
 *
 * @prop value - the percentage in percentage points
 */
export function PercentageOutput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} fractionDigits={fractionDigits} />;
}

/**
 * Component to input a percentage in percentage points.
 *
 * @prop value - the percentage in percentage points
 * @prop onChange - callback when the value changes
 */
export function PercentageInput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...rest} unit={unit} fractionDigits={fractionDigits} />;
}

/**
 * Form field to input a percentage in percentage points.
 */
export function PercentageInputFormField<T extends ZodObject>({
  fractionDigits = 2,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} unit={unit} fractionDigits={fractionDigits} />;
}
