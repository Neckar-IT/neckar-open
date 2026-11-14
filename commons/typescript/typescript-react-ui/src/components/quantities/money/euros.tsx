import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import {
  AdjustableValueNumberInputFormField,
  type AdjustableValueNumberInputFormFieldProps,
} from "../../forms/adjustable-value-number-input-form-field";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "€";
const conversionFactor = conversionFactors.money.toEuro;

/**
 * Displays an amount of money in Euros
 *
 * @prop value - the amount of money in cents
 */
export function EuroOutput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Component to input an amount of money in Euros
 *
 * @prop value - the amount of money in cents
 * @prop onChange - callback when the value changes
 */
export function EuroInput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to input an amount of money in Euros
 */
export function EuroInputFormField<T extends ZodObject>({
  fractionDigits = 2,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to input an amount of money in Euros that the system calculates and a user may override
 *
 * The euro counterpart of [AdjustableValueNumberInputFormField], and the same wrapper around it that
 * [EuroInputFormField] is around the plain number field: the unit and the cents-to-euro factor live
 * here rather than at every call site.
 */
export function AdjustableValueEuroInputFormField<T extends ZodObject>({
  fractionDigits = 2,
  ...rest
}: StripUnitProps<AdjustableValueNumberInputFormFieldProps<T>>): React.ReactElement {
  return <AdjustableValueNumberInputFormField {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}
