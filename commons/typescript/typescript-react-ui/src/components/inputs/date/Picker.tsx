/**
 * Imports
 */

import { type TextKey, useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Calendar } from "../../primitives/calendar";
import { PickerShell, toDisabledMatchers } from "../picker-shell";

/**
 * Type Declarations
 */

interface TextKeys {
  clearSelection: TextKey;
  placeholder: TextKey;
}

export interface DatePickerProps {
  value: Date | undefined;
  onChange: (value: Date | undefined) => void;
  className?: string | undefined;
  min?: Date | undefined;
  max?: Date | undefined;
  quickSelects?: { title: string; date: Date }[] | undefined;
  disabled?: boolean | undefined;
  defaultMonth?: Date | undefined;
  initiallyOpen?: boolean | undefined;
  readOnly?: boolean | undefined;
  isInvalid?: boolean;
  textKeys?: Partial<TextKeys>;
  /** Override the default trigger button. Wrapped in `PopoverTrigger asChild`. */
  trigger?: React.ReactNode | undefined;
  /** The accessible name of the default trigger where no visible label names it. */
  ariaLabel?: string | undefined;
}

/**
 * Calendar-day picker for `LocalDate` fields (the rare exception — see ADR 0150). For
 * `Instant`/DateTime fields, use `DateTimePicker`. Use `DatePickerFormField` for form
 * integration; this bare component's `Date` is anchored at local-midnight and must not be
 * passed through `toISOString()` for day extraction.
 */

export function DatePicker({
  value,
  onChange,
  className,
  min,
  max,
  quickSelects,
  disabled = false,
  defaultMonth,
  initiallyOpen = false,
  readOnly = false,
  isInvalid,
  textKeys,
  trigger,
  ariaLabel,
}: DatePickerProps) {
  const dateFormatter = useDateTimeFormatter("date-with-weekday");
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: TextKeys = {
    clearSelection: textKeys?.clearSelection ?? "datePicker.clearSelection",
    placeholder: textKeys?.placeholder ?? "datePicker.placeholder",
  };

  const label = value === undefined ? t(textKeysWithDefaults.placeholder) : dateFormatter(value);

  return (
    <PickerShell
      ariaLabel={ariaLabel}
      className={className}
      clearSelectionLabel={t(textKeysWithDefaults.clearSelection)}
      disabled={disabled}
      hasValue={value !== undefined}
      initiallyOpen={initiallyOpen}
      isInvalid={isInvalid}
      quickSelects={quickSelects?.map((quickSelect) => ({ title: quickSelect.title, onSelect: () => onChange(quickSelect.date) }))}
      readOnly={readOnly}
      trigger={trigger}
      triggerLabel={label}
      triggerTestId="selected-date"
      onClear={() => onChange(undefined)}
    >
      {(close) => (
        <Calendar
          autoFocus
          defaultMonth={defaultMonth}
          disabled={toDisabledMatchers(min, max)}
          mode="single"
          numberOfMonths={1}
          selected={value}
          onSelect={(selected) => {
            onChange(selected);
            close();
          }}
        />
      )}
    </PickerShell>
  );
}
