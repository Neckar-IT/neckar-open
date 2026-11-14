import { cn } from "cn";
import { type ZodObject } from "zod";
import { FormFieldBase, type FormFieldBaseProps } from "../../forms/form";
import { type DateRange, DateRangePicker, type DateRangePickerProps } from "./Picker";

export interface DateRangePickerFormFieldProps<T extends ZodObject>
  extends FormFieldBaseProps<T>, Omit<DateRangePickerProps, "value" | "onChange"> {}

/**
 * Form-field wrapper around `DateRangePicker` for `LocalDate` range fields (rare — see
 * ADR 0150). For `Instant` ranges, use two `DateTimePickerFormField`s with start-of-day /
 * end-of-day defaults.
 *
 * Form-state shape is `DateRange { from: Date; to: Date }` — out of step with
 * `DatePickerFormField`'s ISO-date-string form-state; symmetrizing is separate work.
 */
export function DateRangePickerFormField<T extends ZodObject>({
  quickSelects,
  min,
  max,
  defaultMonth,
  initiallyOpen,
  textKeys,
  ...rest
}: DateRangePickerFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field, fieldState }) => {
        const value = field.value as DateRange | null | undefined;
        return (
          <DateRangePicker
            value={value ?? undefined}
            onChange={(next) => field.onChange(next ?? null)}
            quickSelects={quickSelects}
            min={min}
            max={max}
            defaultMonth={defaultMonth}
            initiallyOpen={initiallyOpen}
            textKeys={textKeys}
            className={cn({ "border-destructive focus-visible:ring-destructive/30": fieldState.invalid })}
          />
        );
      }}
    />
  );
}
