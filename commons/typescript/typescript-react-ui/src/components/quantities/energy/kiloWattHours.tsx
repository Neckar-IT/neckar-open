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

const unit = "kWh";
const conversionFactor = conversionFactors.energy.toKiloWattHours;

/**
 * Displays an amount of energy in KiloWattHours [kWh]
 *
 * @prop value - the amount of energy in kiloWattHours
 */
export function KiloWattHoursOutput(props: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Component to input an amount of energy in kiloWattHours [kWh]
 *
 * @prop value - the amount of energy in kiloWattHours
 * @prop onChange - callback when the value changes
 */
export function KiloWattHoursInput(props: StripUnitProps<NumberInputProps>): React.ReactElement {
  return <NumberInput {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input an amount of energy in kiloWattHours [kWh]
 */
export function KiloWattHoursInputFormField<T extends ZodObject>(props: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}

/**
 * Form Field to input an amount of energy in kiloWattHours [kWh] that the system calculates and a
 * user may override
 *
 * The kWh counterpart of [AdjustableValueNumberInputFormField], and the same wrapper around it that
 * [KiloWattHoursInputFormField] is around the plain number field: the unit lives here rather than at
 * every call site.
 */
export function AdjustableValueKiloWattHoursInputFormField<T extends ZodObject>(
  props: StripUnitProps<AdjustableValueNumberInputFormFieldProps<T>>,
): React.ReactElement {
  return <AdjustableValueNumberInputFormField {...props} unit={unit} conversionFactor={conversionFactor} />;
}
