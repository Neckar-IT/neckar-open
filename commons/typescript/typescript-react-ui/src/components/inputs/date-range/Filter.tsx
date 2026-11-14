import { useDateTimeFormatter } from "@neckar-hub/typescript-react-i18n";
import { useNow } from "@neckar-hub/typescript-react-utils/providers";
import { cn } from "cn";
import { CalendarIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import type { RangeBounds } from "../../filters/filter-by-number-range";
import { FilterTrigger, useRangeText } from "../../filters/filter-trigger";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { DatePicker } from "../date";

/**
 * Where a picked day lands: `Date` keeps local midnight for a `LocalDate` column, `DateTime` takes the start of the
 * first and the end of the last day for an `Instant` column.
 */
export type DateRangeResolution = "Date" | "DateTime";

/** An inclusive range of days; an open end is `undefined`. */
export interface OpenDateRange {
  from?: Date | undefined;
  to?: Date | undefined;
}

export interface FilterByDateRangeProps {
  /** Translated. */
  label: string;
  value: OpenDateRange;
  onChange: (value: OpenDateRange) => void;
  /** `Inline` is a filter button with presets and two dates in a popover, `Panel` the same without the button. Defaults to `Inline`. */
  form?: "Inline" | "Panel";
  /** Defaults to `Date`. */
  resolution?: DateRangeResolution;
  /** The ends the filter has a parameter for; a one-sided bound shows one date and no presets. Defaults to `Both`. */
  bounds?: RangeBounds;
  className?: string | undefined;
}

/**
 * Filter control for an inclusive range of calendar days with the presets "Letzte 7 Tage", "Letzte 30 Tage" and
 * "Dieser Monat"; "today" comes from `NowProvider`.
 *
 * Use it for `LocalDate` columns; `FilterByDateTimeRange` is the variant for `Instant` columns.
 */
export function FilterByDateRange({ form = "Inline", resolution = "Date", ...props }: FilterByDateRangeProps): React.ReactElement {
  const valueText = useDateRangeText()(props.value);

  switch (form) {
    case "Inline":
      return (
        <Popover>
          <PopoverTrigger asChild>
            <FilterTrigger
              icon={CalendarIcon}
              label={props.label}
              valueText={valueText}
              active={valueText !== undefined}
              onClear={() => props.onChange({})}
              className={props.className}
            />
          </PopoverTrigger>
          <PopoverContent align="start" className="w-auto max-w-[min(24rem,calc(100vw-2rem))] p-3">
            <DateRangeFields {...props} resolution={resolution} />
          </PopoverContent>
        </Popover>
      );
    case "Panel":
      return <DateRangeFields {...props} resolution={resolution} />;
    default:
      throw new Error(`Unexpected value in Switch Statement: form`);
  }
}

function DateRangeFields({
  label,
  value,
  onChange,
  resolution,
  bounds = "Both",
  className,
}: Omit<FilterByDateRangeProps, "form"> & { resolution: DateRangeResolution }): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const today = startOfDay(useNow());

  const setRange = (from: Date | undefined, to: Date | undefined) => {
    switch (resolution) {
      case "Date":
        onChange({ from: from === undefined ? undefined : startOfDay(from), to: to === undefined ? undefined : startOfDay(to) });
        break;
      case "DateTime":
        onChange({ from: from === undefined ? undefined : startOfDay(from), to: to === undefined ? undefined : endOfDay(to) });
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: resolution`);
    }
  };

  const presets = [
    { key: "filterBar.presets.last7Days", from: addDays(today, -6) },
    { key: "filterBar.presets.last30Days", from: addDays(today, -29) },
    { key: "filterBar.presets.thisMonth", from: new Date(today.getFullYear(), today.getMonth(), 1) },
  ];

  return (
    <div className={cn("flex flex-col gap-3", className)}>
      {bounds !== "Both" ? null : (
        <div className="flex flex-wrap gap-2">
          {presets.map((preset) => (
            <button
              key={preset.key}
              type="button"
              className="border-input bg-background hover:border-primary/40 focus-visible:ring-ring/50 min-h-9 rounded-full border px-3 text-sm outline-none focus-visible:ring-3"
              onClick={() => setRange(preset.from, today)}
            >
              {t(preset.key)}
            </button>
          ))}
        </div>
      )}
      <div className="flex items-center gap-2">
        {bounds === "Upper" ? null : (
          <DatePicker
            className="h-11 min-w-36 flex-1 md:h-9"
            ariaLabel={t({ key: "filterBar.lowerBound", options: { label } })}
            value={value.from === undefined ? undefined : startOfDay(value.from)}
            max={value.to}
            onChange={(from) => setRange(from, value.to)}
          />
        )}
        {bounds === "Both" ? <span aria-hidden>–</span> : null}
        {bounds === "Lower" ? null : (
          <DatePicker
            className="h-11 min-w-36 flex-1 md:h-9"
            ariaLabel={t({ key: "filterBar.upperBound", options: { label } })}
            value={value.to === undefined ? undefined : startOfDay(value.to)}
            min={value.from}
            onChange={(to) => setRange(value.from, to)}
          />
        )}
      </div>
    </div>
  );
}

/**
 * The text of a date range in a filter button and in a chip, e.g. "01.09.2026 – 30.09.2026".
 */
export function useDateRangeText(): (value: OpenDateRange) => string | undefined {
  const rangeText = useRangeText();
  const formatDate = useDateTimeFormatter("date");
  return (value) => rangeText(formatDate(value.from), formatDate(value.to));
}

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function endOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}
