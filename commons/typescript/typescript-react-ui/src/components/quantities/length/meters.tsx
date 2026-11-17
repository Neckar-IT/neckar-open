import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "m";
const conversionFactor = conversionFactors.length.toMeters;

/**
 * Displays a length in Meters
 *
 * @prop value - the length in meters
 */
export function MeterOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Component to input a length in Meters
 *
 * @prop value - the length in meters
 * @prop onChange - callback when the value changes
 */
export function MeterInput(props: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input a length in Meters
 */
export function MeterInputFormField<T extends ZodObject>(props: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
