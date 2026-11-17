import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "V";
const conversionFactor = conversionFactors.voltage.toVolt;

/**
 * Displays a voltage in Volt [V]
 *
 * @prop value - the power in V
 */
export function VoltOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Component to input a voltage in Volt [V]
 *
 * @prop value - the voltage value in V
 * @prop onChange - callback when the value changes
 */
export function VoltInput(props: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input a voltage in Volt [V]
 */
export function VoltInputFormField<T extends ZodObject>(props: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
