import { type ZodObject } from "zod";
import { FormFieldBase, type FormFieldBaseProps } from "../../forms/form";
import { type DefaultTimeOfDay } from "./defaultTimeOfDay";
import { DateTimePicker } from "./Picker";

export type { DefaultTimeOfDay };

export interface DateTimePickerFormFieldProps<T extends ZodObject> extends FormFieldBaseProps<T, Date | null | undefined> {
  min?: Date;
  max?: Date;
  /**
   * Quick-select shortcuts rendered next to the calendar inside the popover. Each entry's
   * full `date` (date + time) is adopted verbatim — `defaultTimeOfDay` is bypassed for
   * quick-selects so a preset like "tomorrow 09:00" stays at 09:00.
   */
  quickSelects?: { title: string; date: Date }[];
  /**
   * Default time-of-day applied on the very first calendar pick (when no `value` is
   * set yet). Once any value exists, picking a different day preserves the existing
   * time. Defaults to `"start"` — pass `"end"` for "to"-bound semantics.
   */
  defaultTimeOfDay?: DefaultTimeOfDay;
}

/**
 * Form-field wrapper around `DateTimePicker`. Translates the form-state convention
 * (`null` for empty) to the picker convention (`undefined` for empty) at the seam.
 */
export function DateTimePickerFormField<T extends ZodObject>({
  min,
  max,
  quickSelects,
  defaultTimeOfDay = "start",
  ...rest
}: DateTimePickerFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => {
        const value = field.value as Date | null | undefined;
        return (
          <DateTimePicker
            value={value ?? undefined}
            onChange={(next) => field.onChange(next ?? null)}
            min={min}
            max={max}
            quickSelects={quickSelects}
            defaultTimeOfDay={defaultTimeOfDay}
            disabled={field.disabled}
            isInvalid={fieldState.invalid}
          />
        );
      }}
    />
  );
}
