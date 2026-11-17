import { type ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { NumberInput, type NumberInputProps } from "../../inputs/number-input";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "s";

/**
 * Displays a duration in Seconds [s]
 *
 * A duration crosses the wire as ISO-8601 text, not in the base unit of time, so the caller parses
 * it into seconds and nothing converts here.
 *
 * @prop value - the duration in seconds
 */
export function SecondsOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} />;
}

/**
 * Component to input a duration in Seconds [s]
 *
 * @prop value - the duration in seconds
 * @prop onChange - callback when the value changes
 */
export function SecondsInput({ fractionDigits = 3, ...rest }: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...rest} unit={unit} fractionDigits={fractionDigits} />;
}

/**
 * Form Field to input a duration in Seconds [s]
 */
export function SecondsInputFormField<T extends ZodObject>({
  fractionDigits = 3,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} unit={unit} fractionDigits={fractionDigits} />;
}
