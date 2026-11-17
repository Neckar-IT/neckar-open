/**
 * Imports
 */

import type { ZodObject } from "zod";
import { NumberInput } from "../../inputs/number-input";
import { FormFieldBase, type FormFieldBaseProps } from "../form";

/**
 * Prop Types
 */

export interface NumberInputFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T> {
  step?: number;
  min?: number;
  max?: number;
  /**
   * Number of fraction digits to allow (e.g. 2 allows "0.00", 3 allows "0.000")
   */
  fractionDigits?: number;
  /**
   * Unit to display next to the number input (e.g. "kg", "cm", "%")
   */
  unit?: string;
  /**
   * Position of the unit - defaults to `suffix`
   */
  unitPosition?: "prefix" | "suffix";
  conversionFactor?: number;
  /**
   * If true, shows increment and decrement buttons.
   * Defaults to `false`.
   */
  showButtons?: boolean;
  hideClearButton?: boolean;
  /**
   * Whether to use grouping separators (e.g. "1.000" vs "1000").
   * Defaults to `true`. Set to `false` for values like years where grouping is misleading.
   */
  useGrouping?: boolean;
  /** Shown while the field is empty; without a `label`, it also names the field for assistive technology. */
  placeholder?: string;
}

/**
 * Form-field wrapper around `NumberInput`. Translates the form-state convention
 * (`null` for empty) to the picker convention (`undefined` for empty) at the seam.
 */

export function NumberInputFormField<T extends ZodObject>({
  step,
  min,
  max,
  fractionDigits,
  unit,
  unitPosition,
  conversionFactor,
  showButtons,
  hideClearButton,
  useGrouping,
  placeholder,
  ...rest
}: NumberInputFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => {
        const value = field.value as number | null | undefined;
        return (
          <NumberInput
            value={value ?? undefined}
            onChange={(next) => field.onChange(next ?? null)}
            step={step}
            min={min}
            max={max}
            fractionDigits={fractionDigits}
            unit={unit}
            unitPosition={unitPosition}
            conversionFactor={conversionFactor}
            showButtons={showButtons}
            hideClearButton={hideClearButton}
            useGrouping={useGrouping}
            placeholder={placeholder}
            ariaLabel={rest.label === undefined ? placeholder : undefined}
            disabled={field.disabled}
            isInvalid={fieldState.invalid}
          />
        );
      }}
    />
  );
}
