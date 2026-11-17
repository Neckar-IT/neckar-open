import type { ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { NumberInputFormField, type NumberInputFormFieldProps } from "../../forms/number-input-form-field/NumberInputFormField";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

// The Kotlin `Fraction` travels as a fraction; the reader sees percent, as `VatRateOutput` shows a VAT rate.
const unit = "%";
const conversionFactor = conversionFactors.fraction.toPercent;
// `NumberInput` adds `step` to the held fraction, so its default of 1 would move an arrow key by 100 percentage points.
const onePercentagePoint = 1 / conversionFactor;

/** Displays a fraction in percent: `0.05` renders as "5.00 %". */
export function FractionOutput({ fractionDigits = 2, ...rest }: StripUnitProps<NumberOutputProps>): React.ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/** Form field that takes percent and holds the fraction. */
export function FractionInputFormField<T extends ZodObject>({
  fractionDigits = 2,
  step = onePercentagePoint,
  ...rest
}: StripUnitProps<NumberInputFormFieldProps<T>>): React.ReactElement {
  return <NumberInputFormField {...rest} step={step} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}
