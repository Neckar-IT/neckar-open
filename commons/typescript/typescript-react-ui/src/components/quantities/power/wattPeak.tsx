import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "Wp";
const conversionFactor = conversionFactors.power.toWatt;

/**
 * Displays a peak power value in Watt Peak [Wp]
 *
 * @prop value - the power in Wp
 */
export function WattPeakOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Component to input a peak power value in Watt Peak [Wp]
 *
 * @prop value - the peak power value in Wp
 * @prop onChange - callback when the value changes
 */
export function WattPeakInput(props: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input a peak power value in Watt Peak [Wp]
 */
export function WattPeakInputFormField<T extends ZodObject>(props: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
