import { parseLocalDate, toLocalDate } from "@neckar-hub/typescript-utils/date";
import type { ZodObject } from "zod";
import { FormFieldBase, type FormFieldBaseProps } from "../../forms/form";
import { DatePicker, type DatePickerProps } from "./Picker";

/**
 * Prop Types
 */
export interface DatePickerFormFieldProps<T extends ZodObject>
  extends FormFieldBaseProps<T, string | null | undefined>, Omit<DatePickerProps, "value" | "onChange"> {}

/**
 * Form-field wrapper around `DatePicker` for `LocalDate` fields (rare — see ADR 0150). For
 * `Instant`/DateTime fields, use `DateTimePickerFormField`. Form-state value is an ISO-8601
 * date string `yyyy-MM-dd`; validate with `z.iso.date()` — `fieldName` only accepts schema
 * fields of that shape, so a `z.date()` field is a compile error rather than a render crash.
 */
export function DatePickerFormField<T extends ZodObject>({
  min,
  max,
  quickSelects,
  defaultMonth,
  initiallyOpen,
  readOnly,
  textKeys,
  ...rest
}: DatePickerFormFieldProps<T>) {
  return (
    <FormFieldBase
      {...rest}
      render={({ field: { value, onChange, ...fieldRest }, fieldState }) => {
        // The empty text is no calendar day — it is the state a required date field starts in, and
        // `parseLocalDate` throws on anything that is not `yyyy-MM-dd`.
        const formValue = (value as string | null | undefined) || undefined;
        return (
          <DatePicker
            {...fieldRest}
            value={parseLocalDate(formValue)}
            onChange={(date) => onChange(date === undefined ? null : toLocalDate(date))}
            min={min}
            max={max}
            quickSelects={quickSelects}
            defaultMonth={defaultMonth}
            initiallyOpen={initiallyOpen}
            textKeys={textKeys}
            readOnly={readOnly}
            isInvalid={fieldState.invalid}
          />
        );
      }}
    />
  );
}
