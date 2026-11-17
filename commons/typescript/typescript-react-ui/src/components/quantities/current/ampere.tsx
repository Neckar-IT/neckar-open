import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "A";
const conversionFactor = conversionFactors.current.toAmpere;

/**
 * Displays an electrical current in Ampere [A]
 *
 * @prop value - the current in A
 */
export function AmpereOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Component to input an electrical current in Ampere [A]
 *
 * @prop value - the current in Ampere [A]
 * @prop onChange - callback when the value changes
 */
export function AmpereInput(props: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input an electrical current in Ampere [A]
 */
export function AmpereInputFormField<T extends ZodObject>(props: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
