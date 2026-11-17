import { type TextKey, useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { Calendar } from "../../primitives/calendar";
import { Separator } from "../../primitives/separator";
import { PickerShell, toDisabledMatchers } from "../picker-shell";
import { TimeInput } from "../time";
import { combineDayAndTime, type DefaultTimeOfDay, matchesDefaultTimeOfDay } from "./defaultTimeOfDay";

interface TextKeys {
  /** Trigger text shown when no value is set. */
  placeholder: TextKey;
  /** Inline button below the time input, shown when a value is set. */
  clearSelection: TextKey;
}

export interface DateTimePickerProps {
  value: Date | undefined;
  onChange: (value: Date | undefined) => void;
  className?: string | undefined;
  /** Inclusive lower bound on the **calendar day** of the picker. The time portion is not constrained. */
  min?: Date | undefined;
  /** Inclusive upper bound on the **calendar day** of the picker. The time portion is not constrained. */
  max?: Date | undefined;
  /** Quick-select shortcuts rendered next to the calendar. The full `date` (date + time) is adopted verbatim. */
  quickSelects?: { title: string; date: Date }[] | undefined;
  disabled?: boolean | undefined;
  /** Calendar month to show on first open when no value is set. */
  defaultMonth?: Date | undefined;
  /** Open the popover on first render — for tests / story-book. */
  initiallyOpen?: boolean | undefined;
  /** Default time-of-day applied on the very first calendar click (when `value` is undefined). Defaults to `"start"`. */
  defaultTimeOfDay?: DefaultTimeOfDay | undefined;
  /** Override the default trigger button. Wrapped in `PopoverTrigger asChild`. */
  trigger?: React.ReactNode | undefined;
  textKeys?: Partial<TextKeys> | undefined;
  isInvalid?: boolean | undefined;
  readOnly?: boolean | undefined;
  /** The accessible name of the default trigger where no visible label names it. */
  ariaLabel?: string | undefined;
}

/**
 * Combined calendar + time-of-day picker — a single popover trigger that opens onto a
 * `Calendar` for the date and a `TimeInput` for the time portion of an `Instant` value.
 *
 * The default trigger renders a `Button` with the formatted date-and-time (or the
 * placeholder when empty); pass `trigger` to swap in a custom-styled trigger.
 *
 * On the very first calendar click without a prior value, the time portion is seeded
 * from `defaultTimeOfDay` — `"start"` for "from"-bounds (00:00), `"end"` for "to"-bounds
 * (23:59:59.999), `"middle"` for single picks (12:00). Subsequent day picks preserve
 * whatever time has already been set.
 *
 * Use this for inputs whose backing DB column is an `Instant`. For calendar-day inputs
 * over a `LocalDate` column, use `DatePicker` instead.
 */
export function DateTimePicker({
  value,
  onChange,
  className,
  min,
  max,
  quickSelects,
  disabled,
  defaultMonth,
  initiallyOpen = false,
  defaultTimeOfDay = "start",
  trigger,
  textKeys,
  isInvalid,
  readOnly,
  ariaLabel,
}: DateTimePickerProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const dateTimeFormatter = useDateTimeFormatter("date-and-time");
  const dateFormatter = useDateTimeFormatter("date");
  const timeFormatter = useDateTimeFormatter("time");

  const placeholderKey: TextKey = textKeys?.placeholder ?? "datePicker.placeholder";
  const clearSelectionKey: TextKey = textKeys?.clearSelection ?? "datePicker.clearSelection";

  const handleDateSelect = (next: Date | undefined) => {
    if (next === undefined) {
      onChange(undefined);
      return;
    }
    onChange(combineDayAndTime(next, value, defaultTimeOfDay));
  };

  const handleTimeChange = (timeString: string | null) => {
    if (timeString === null) {
      onChange(undefined);
      return;
    }
    if (value === undefined) return;
    onChange(applyTime(value, timeString));
  };

  const label =
    value === undefined
      ? t(placeholderKey)
      : matchesDefaultTimeOfDay(value, defaultTimeOfDay)
        ? dateFormatter(value)
        : dateTimeFormatter(value);

  return (
    <PickerShell
      ariaLabel={ariaLabel}
      className={className}
      clearSelectionLabel={t(clearSelectionKey)}
      disabled={disabled}
      hasValue={value !== undefined}
      initiallyOpen={initiallyOpen}
      isInvalid={isInvalid}
      // Quick-select carries a complete Date — adopt date AND time verbatim
      // (e.g. "tomorrow at 09:00" stays at 09:00, ignoring defaultTimeOfDay).
      quickSelects={quickSelects?.map((quickSelect) => ({ title: quickSelect.title, onSelect: () => onChange(quickSelect.date) }))}
      readOnly={readOnly}
      trigger={trigger}
      triggerLabel={label}
      onClear={() => onChange(undefined)}
    >
      <div>
        <Calendar
          autoFocus
          defaultMonth={defaultMonth ?? value ?? min ?? max}
          disabled={toDisabledMatchers(min !== undefined ? stripTime(min) : undefined, max !== undefined ? stripTime(max) : undefined)}
          mode="single"
          numberOfMonths={1}
          selected={value}
          onSelect={handleDateSelect}
        />
        <Separator orientation="horizontal" />
        <div className="p-3">
          <TimeInput hideClearButton value={value !== undefined ? timeFormatter(value) : null} onChange={handleTimeChange} />
        </div>
      </div>
    </PickerShell>
  );
}

function stripTime(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function applyTime(base: Date, timeString: string): Date {
  const match = /^(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(timeString);
  if (match === null) return base;
  const [, hours, minutes, seconds] = match;
  const result = new Date(base);
  result.setHours(Number(hours), Number(minutes), seconds !== undefined ? Number(seconds) : 0, 0);
  return result;
}
