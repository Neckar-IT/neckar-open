/**
 * Imports
 */

import { type TextKey, useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";
import { useMemo } from "react";
import { type DateRange as DayPickerDateRange } from "react-day-picker";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Calendar } from "../../primitives/calendar";
import { PickerShell, toDisabledMatchers } from "../picker-shell";

/**
 * Type Declarations
 */

export interface DateRange {
  from: Date;
  to: Date;
}

interface DateRangePickerTextKeys {
  clearSelection: TextKey;
  placeholder: TextKey;
}

export interface DateRangePickerProps {
  value: DateRange | undefined;
  onChange: (value: DateRange | undefined) => void;
  className?: string | undefined;
  quickSelects?: { title: string; from: Date; to: Date }[] | undefined;
  min?: Date | undefined;
  max?: Date | undefined;
  disabled?: boolean | undefined;
  defaultMonth?: Date | undefined;
  initiallyOpen?: boolean | undefined;
  readOnly?: boolean | undefined;
  isInvalid?: boolean | undefined;
  textKeys?: Partial<DateRangePickerTextKeys>;
  /** Override the default trigger button. Wrapped in `PopoverTrigger asChild`. */
  trigger?: React.ReactNode | undefined;
  /** The accessible name of the default trigger where no visible label names it. */
  ariaLabel?: string | undefined;
}

/**
 * Calendar-day range picker for `LocalDate` fields (the rare exception — see ADR 0150). For
 * `Instant`/DateTime ranges, use two `DateTimePicker`s with start-of-day / end-of-day
 * defaults. Each `Date` in `DateRange { from, to }` is anchored at local-midnight and must
 * not be passed through `toISOString()` for day extraction.
 */

export function DateRangePicker({
  value,
  onChange,
  className,
  quickSelects,
  min,
  max,
  disabled,
  defaultMonth = new Date(),
  initiallyOpen = false,
  readOnly,
  isInvalid,
  textKeys,
  trigger,
  ariaLabel,
}: DateRangePickerProps) {
  const dateFormatter = useDateTimeFormatter("date");
  const { t } = useEmbeddedTranslation();
  const textKeysWithDefaults: DateRangePickerTextKeys = {
    clearSelection: textKeys?.clearSelection ?? "dateRangePicker.clearSelection",
    placeholder: textKeys?.placeholder ?? "dateRangePicker.placeholder",
  };

  const handleSelect = createRangeSelectHandler(onChange);

  const valueDisplay = useMemo(() => {
    if (value === undefined) return t(textKeysWithDefaults.placeholder);

    if (
      value.from.getFullYear() === value.to.getFullYear() &&
      value.from.getMonth() === value.to.getMonth() &&
      value.from.getDate() === value.to.getDate()
    ) {
      return dateFormatter(value.from);
    }

    return `${dateFormatter(value.from)} - ${dateFormatter(value.to)}`;
  }, [value, t, textKeysWithDefaults.placeholder, dateFormatter]);

  return (
    <PickerShell
      ariaLabel={ariaLabel}
      className={className}
      clearSelectionLabel={t(textKeysWithDefaults.clearSelection)}
      disabled={disabled}
      hasValue={value !== undefined}
      initiallyOpen={initiallyOpen}
      isInvalid={isInvalid}
      quickSelects={quickSelects?.map((quickSelect) => ({
        title: quickSelect.title,
        onSelect: () => {
          handleSelect({ from: quickSelect.from, to: quickSelect.to });
        },
      }))}
      readOnly={readOnly}
      trigger={trigger}
      triggerLabel={valueDisplay}
      triggerTestId="selected-date-range"
      onClear={() => {
        handleSelect(undefined);
      }}
    >
      <Calendar
        autoFocus
        defaultMonth={defaultMonth}
        disabled={toDisabledMatchers(min, max)}
        mode="range"
        numberOfMonths={1}
        selected={value}
        onSelect={handleSelect}
      />
    </PickerShell>
  );
}

/**
 * Turns a calendar range into a `DateRange` from start of the first day to end of the last day.
 */

function createRangeSelectHandler(onChange: (value: DateRange | undefined) => void) {
  const handleSelect = (selected: DayPickerDateRange | null | undefined) => {
    if (selected === undefined || selected === null) {
      onChange(undefined);
      return;
    }

    if (selected.from === undefined && selected.to === undefined) {
      onChange(undefined);
      return;
    }

    if (selected.from !== undefined && selected.to !== undefined) {
      onChange({
        from: new Date(selected.from.setHours(0, 0, 0, 0)),
        to: new Date(selected.to.setHours(23, 59, 59, 999)),
      });
      return;
    }

    if (selected.from !== undefined && selected.to === undefined) {
      onChange({
        from: new Date(selected.from.setHours(0, 0, 0, 0)),
        to: new Date(selected.from.setHours(23, 59, 59, 999)),
      });
      return;
    }

    if (selected.from === undefined && selected.to !== undefined) {
      onChange({
        from: new Date(selected.to.setHours(0, 0, 0, 0)),
        to: new Date(selected.to.setHours(23, 59, 59, 999)),
      });
    }
  };

  return handleSelect;
}
