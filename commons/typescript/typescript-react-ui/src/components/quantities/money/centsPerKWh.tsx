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

const unit = "ct/kWh";
const conversionFactor = conversionFactors.money.toCent;

/**
 * Displays price per kilowatt-hour in Cents
 *
 * @prop value - the price in cents
 */
export function CentsPerKWhOutput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Component to input a price per kilowatt-hour in Cents
 *
 * @prop value - the price in cents
 * @prop onChange - callback when the value changes
 */
export function CentsPerKWhInput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to input a price per kilowatt-hour in Cents
 */
export function CentsPerKWhInputFormField<T extends ZodObject>({
  fractionDigits = 2,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to edit an adjustable value in Cents per kilowatt-hour
 */
export function AdjustableValueCentsPerKWhInputFormField<T extends ZodObject>(
  props: StripUnitProps<AdjustableValueNumberInputFormFieldProps<T>>,
): React.ReactElement {
  return <AdjustableValueNumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
