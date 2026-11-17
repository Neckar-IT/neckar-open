import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "kW";
const conversionFactor = conversionFactors.power.toKiloWatt;

/**
 * Displays a power value in KiloWatt [kW]
 *
 * @prop value - the power in W
 */
export function KiloWattOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Component to input a power value in KiloWatt [kW]
 *
 * @prop value - the peak power value in W
 * @prop onChange - callback when the value changes
 */
export function KiloWattInput(props: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input a power value in KiloWatt [kW]
 */
export function KiloWattInputFormField<T extends ZodObject>(props: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
