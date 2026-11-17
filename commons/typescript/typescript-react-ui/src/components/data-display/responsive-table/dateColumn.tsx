import type { ReactNode } from "react";
import { FormattedDate } from "../formatted-date";
import { FormattedDateTime } from "../formatted-date-time";
import type { ResponsiveColumnDefinition } from "./ResponsiveTable";

type DateValue = Date | string | null | undefined;

type DateColumnOptions<ItemType, SortCriteria> = Omit<ResponsiveColumnDefinition<ItemType, SortCriteria>, "cell" | "header"> & {
  header: ReactNode;
  accessor: (item: ItemType) => DateValue;
};

function buildDateColumn<ItemType, SortCriteria>(
  options: DateColumnOptions<ItemType, SortCriteria>,
  renderCell: (date: DateValue) => ReactNode,
): ResponsiveColumnDefinition<ItemType, SortCriteria> {
  const { accessor, ...column } = options;
  return {
    ...column,
    cell: (item) => renderCell(accessor(item)),
  } as ResponsiveColumnDefinition<ItemType, SortCriteria>;
}

/**
 * Column helper for `LocalDate` wire values. Renders via `FormattedDate` (locale-aware,
 * date-only). Use `dateTimeColumn` for `Instant` wire values.
 */
export function dateColumn<ItemType, SortCriteria>(
  options: DateColumnOptions<ItemType, SortCriteria>,
): ResponsiveColumnDefinition<ItemType, SortCriteria> {
  return buildDateColumn(options, (date) => <FormattedDate date={date} />);
}

/**
 * Column helper for `Instant` wire values. Renders via `FormattedDateTime` (locale-aware,
 * date + time). Use `dateColumn` for `LocalDate` wire values.
 */
export function dateTimeColumn<ItemType, SortCriteria>(
  options: DateColumnOptions<ItemType, SortCriteria>,
): ResponsiveColumnDefinition<ItemType, SortCriteria> {
  return buildDateColumn(options, (date) => <FormattedDateTime date={date} />);
}
