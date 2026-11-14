import { FilterByNumberRange, type NumberRange, type RangeBounds } from "../filter-by-number-range";

interface FilterByYearProps {
  /** Translated. */
  label: string;
  value: NumberRange;
  onChange: (value: NumberRange) => void;
  /** Defaults to `Inline`. */
  form?: "Inline" | "Panel";
  /** Defaults to `Both`. */
  bounds?: RangeBounds;
}

/**
 * Filter control for an inclusive range of years.
 */
export function FilterByYear(props: FilterByYearProps): React.ReactElement {
  return <FilterByNumberRange {...props} numberKind="Year" />;
}
