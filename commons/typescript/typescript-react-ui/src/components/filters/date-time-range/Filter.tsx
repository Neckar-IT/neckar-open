import { FilterByDateRange, type FilterByDateRangeProps } from "../../inputs/date-range";

export type FilterByDateTimeRangeProps = Omit<FilterByDateRangeProps, "resolution">;

/**
 * Filter control for an inclusive range over an `Instant` column: a picked day sets `from` to its start and `to` to
 * its end, so one click covers whole days.
 */
export function FilterByDateTimeRange(props: FilterByDateTimeRangeProps): React.ReactElement {
  return <FilterByDateRange {...props} resolution="DateTime" />;
}
