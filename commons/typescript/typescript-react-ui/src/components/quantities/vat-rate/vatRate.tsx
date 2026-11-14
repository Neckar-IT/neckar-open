/**
 * Components to select and display a VAT rate.
 *
 * A VAT rate is a fraction: 0.19 is 19 %. That is the shape of the Kotlin value class
 * `it.neckar.financial.currency.VatRate` and therefore the shape on the wire — the components take
 * and return the fraction, no conversion happens anywhere.
 *
 * Supports the German VAT rates: 19 %, 7 % and 0 %.
 */

import { usePercentageFormatter } from "@neckar-hub/typescript-react-i18n";
import type { ReactElement } from "react";
import type { ZodObject } from "zod";
import { NumberOutput, type NumberOutputProps } from "../../data-display/number-output";
import { FormFieldBase, type FormFieldBaseProps } from "../../forms/form";
import { SelectFromList } from "../../inputs/select-from-list";
import { conversionFactors } from "../conversionFactors";
import { type StripUnitProps } from "../StripUnitProps";

const unit = "%";
const conversionFactor = conversionFactors.fraction.toPercent;

/**
 * VAT rate options for the select field, as fractions.
 */
export const vatRateOptions: number[] = [0.19, 0.07, 0.0];

/**
 * Displays a VAT rate given as a fraction (0.19) as a percentage ("19 %").
 *
 * @prop value - the VAT rate as a fraction
 */
export function VatRateOutput({ fractionDigits = 0, ...rest }: StripUnitProps<NumberOutputProps>): ReactElement {
  return <NumberOutput {...rest} unit={unit} conversionFactor={conversionFactor} fractionDigits={fractionDigits} />;
}

/**
 * Props for SelectVatRate component.
 */
interface SelectVatRateProps {
  value?: number | null;
  onChange?: (value: number | null) => void;
  placeholder?: string;
  isInvalid?: boolean;
}

/**
 * Component to select a VAT rate. The options are rendered as percentages ("19 %"), the value is the
 * fraction (0.19).
 *
 * @prop value - The current VAT rate as a fraction
 * @prop onChange - Callback when the value changes
 */
export function SelectVatRate({ value, onChange, placeholder, isInvalid }: SelectVatRateProps): ReactElement {
  const formatPercentage = usePercentageFormatter(0);

  return (
    <SelectFromList
      options={vatRateOptions}
      itemRenderer={(option) => formatPercentage(option)}
      value={value}
      onChange={(newValue) => onChange?.(newValue)}
      placeholder={placeholder}
      isInvalid={isInvalid}
    />
  );
}

/**
 * Props for SelectVatRateFormField component.
 */
export type SelectVatRateFormFieldProps<T extends ZodObject> = FormFieldBaseProps<T> & {
  placeholder?: string;
};

/**
 * Form field to select a VAT rate. The form value is the fraction (0.19) — identical to the wire
 * shape of `it.neckar.financial.currency.VatRate`.
 */
export function SelectVatRateFormField<T extends ZodObject>({ placeholder, ...rest }: SelectVatRateFormFieldProps<T>): ReactElement {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => {
        return <SelectVatRate {...field} placeholder={placeholder} isInvalid={fieldState.invalid} />;
      }}
    />
  );
}
