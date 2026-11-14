import { useNumberFormatter } from "@neckar-hub/typescript-react-i18n";
import { cn } from "cn";
import { CalendarRangeIcon, EuroIcon, HashIcon, type LucideIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { NumberInput } from "../../inputs/number-input";
import { Popover, PopoverContent, PopoverTrigger } from "../../primitives/popover";
import { conversionFactors } from "../../quantities/conversionFactors";
import { EuroInput } from "../../quantities/money/euros";
import { activeFilterSurface, FilterTrigger, useRangeText } from "../filter-trigger";

/** An inclusive numeric range; an open end is `undefined`. */
export interface NumberRange {
  min?: number | undefined;
  max?: number | undefined;
}

/** The number an input of a range edits. `EuroCents` is an amount in cents, entered and shown in euro. */
export type NumberRangeKind = "Integer" | "Decimal" | "Year" | "EuroCents";

/** The ends of a range that have a request parameter; a one-sided bound such as "Bewertung ab 4" has one. */
export type RangeBounds = "Both" | "Lower" | "Upper";

export interface FilterByNumberRangeProps {
  /** Translated. */
  label: string;
  value: NumberRange;
  onChange: (value: NumberRange) => void;
  numberKind: NumberRangeKind;
  /** The ends the filter has a parameter for; a one-sided bound shows one field. Defaults to `Both`. */
  bounds?: RangeBounds;
  /** Shown after the bounds; `EuroCents` brings its own. */
  unit?: string | undefined;
  /** `Inline` is a filter button with both fields in a popover, `Panel` the two fields. Defaults to `Inline`. */
  form?: "Inline" | "Panel";
  className?: string | undefined;
}

/**
 * Filter control for an inclusive numeric range (`min`–`max`); an open end stays empty.
 */
export function FilterByNumberRange({ form = "Inline", ...props }: FilterByNumberRangeProps): React.ReactElement {
  const valueText = useNumberRangeText()(props.value, props.numberKind, props.unit);

  switch (form) {
    case "Inline":
      return (
        <Popover>
          <PopoverTrigger asChild>
            <FilterTrigger
              icon={iconOf(props.numberKind)}
              label={props.label}
              valueText={valueText}
              active={valueText !== undefined}
              onClear={() => props.onChange({})}
              className={props.className}
            />
          </PopoverTrigger>
          <PopoverContent align="start" className="w-auto p-3">
            <RangeFields {...props} marked={false} />
          </PopoverContent>
        </Popover>
      );
    case "Panel":
      return <RangeFields {...props} marked />;
    default:
      throw new Error(`Unexpected value in Switch Statement: form`);
  }
}

function RangeFields({
  label,
  value,
  onChange,
  numberKind,
  unit,
  bounds = "Both",
  className,
  marked,
}: FilterByNumberRangeProps & {
  /**
   * The form `Panel`: the fields share the full width, and a set bound shows in the surface of an active filter button
   * with its own X. The popover of the form `Inline` leaves both to the trigger button.
   */
  marked: boolean;
}): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const lower = (
    <BoundInput
      numberKind={numberKind}
      unit={unit}
      ariaLabel={t({ key: "filterBar.lowerBound", options: { label } })}
      value={value.min}
      onChange={(min) => onChange({ ...value, min })}
      lowerBound={undefined}
      marked={marked}
    />
  );
  const upper = (
    <BoundInput
      numberKind={numberKind}
      unit={unit}
      ariaLabel={t({ key: "filterBar.upperBound", options: { label } })}
      value={value.max}
      onChange={(max) => onChange({ ...value, max })}
      lowerBound={value.min}
      marked={marked}
    />
  );

  return (
    <div className={cn("flex items-center gap-2", marked ? "w-full" : null, className)}>
      {bounds === "Upper" ? null : lower}
      {bounds === "Both" ? <span aria-hidden>–</span> : null}
      {bounds === "Lower" ? null : upper}
    </div>
  );
}

interface BoundInputProps {
  numberKind: NumberRangeKind;
  unit: string | undefined;
  ariaLabel: string;
  value: number | undefined;
  onChange: (value: number | undefined) => void;
  /** The upper field takes the lower bound as its minimum. */
  lowerBound: number | undefined;
  marked: boolean;
}

function BoundInput({ numberKind, unit, ariaLabel, value, onChange, lowerBound, marked }: BoundInputProps): React.ReactElement {
  const active = marked && value !== undefined;
  const shared = {
    value,
    onChange,
    ariaLabel,
    min: lowerBound ?? 0,
    hideClearButton: !marked,
    className: cn("h-11 md:h-9", active ? activeFilterSurface : null),
  };
  return (
    <div data-active={active} className={marked ? "min-w-0 flex-1" : "w-32"}>
      {(() => {
        switch (numberKind) {
          case "Integer":
            return <NumberInput {...shared} unit={unit} fractionDigits={0} useGrouping={false} />;
          case "Decimal":
            return <NumberInput {...shared} unit={unit} />;
          case "Year":
            return <NumberInput {...shared} min={lowerBound} fractionDigits={0} useGrouping={false} />;
          case "EuroCents":
            return <EuroInput {...shared} />;
          default:
            throw new Error(`Unexpected value in Switch Statement: numberKind`);
        }
      })()}
    </div>
  );
}

function iconOf(numberKind: NumberRangeKind): LucideIcon {
  switch (numberKind) {
    case "Integer":
    case "Decimal":
      return HashIcon;
    case "Year":
      return CalendarRangeIcon;
    case "EuroCents":
      return EuroIcon;
    default:
      throw new Error(`Unexpected value in Switch Statement: numberKind`);
  }
}

/**
 * The text of a numeric range in a filter button and in a chip, e.g. "100,00 € – 250,00 €".
 */
export function useNumberRangeText(): (value: NumberRange, numberKind: NumberRangeKind, unit: string | undefined) => string | undefined {
  const rangeText = useRangeText();
  const formatEuro = useNumberFormatter(2);
  const formatDecimal = useNumberFormatter();
  const formatPlain = useNumberFormatter(0, false);

  const boundText = (bound: number | undefined, numberKind: NumberRangeKind, unit: string | undefined): string | undefined => {
    if (bound === undefined) {
      return undefined;
    }
    switch (numberKind) {
      case "Integer":
        return withUnit(formatPlain(bound), unit);
      case "Decimal":
        return withUnit(formatDecimal(bound), unit);
      case "Year":
        return formatPlain(bound);
      case "EuroCents":
        return withUnit(formatEuro(bound * conversionFactors.money.toEuro), "€");
      default:
        throw new Error(`Unexpected value in Switch Statement: numberKind`);
    }
  };

  return (value, numberKind, unit) => rangeText(boundText(value.min, numberKind, unit), boundText(value.max, numberKind, unit));
}

function withUnit(text: string, unit: string | undefined): string {
  return unit === undefined ? text : `${text} ${unit}`;
}
