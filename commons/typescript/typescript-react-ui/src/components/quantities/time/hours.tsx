import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "h";
const conversionFactor = conversionFactors.time.toHours;

/**
 * Displays a duration in Hours [h]
 *
 * @prop value - the duration in hours
 */
export function HoursOutput({ fractionDigits = 1, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Component to input a duration in Hours [h]
 *
 * @prop value - the duration in hours
 * @prop onChange - callback when the value changes
 */
export function HoursInput({ fractionDigits = 1, ...rest }: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to input a duration in Hours [h]
 */
export function HoursInputFormField<T extends ZodObject>({
  fractionDigits = 1,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}
