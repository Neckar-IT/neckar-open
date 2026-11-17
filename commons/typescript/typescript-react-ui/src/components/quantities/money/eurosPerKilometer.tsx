import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "€/km";
const conversionFactor = conversionFactors.money.toEuro;

/**
 * Displays a price per kilometre travelled in Euros
 *
 * @prop value - the price per kilometre in cents
 */
export function EuroPerKilometerOutput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Component to input a price per kilometre travelled in Euros
 *
 * @prop value - the price per kilometre in cents
 * @prop onChange - callback when the value changes
 */
export function EuroPerKilometerInput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to input a price per kilometre travelled in Euros
 */
export function EuroPerKilometerInputFormField<T extends ZodObject>({
  fractionDigits = 2,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}
