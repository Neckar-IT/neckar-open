/**
 * Imports
 */

import { useDateTimeSearchParam } from "./useDateTimeSearchParam";
import { useSetterWithPaginationReset } from "./useSetterWithPaginationReset";

/**
 * Hook to manage a single instant (date + time) search parameter that is used as a filter
 * for tables or lists. The wire format is a full ISO-8601 instant — pair with a backend
 * filter over an `Instant` column. Resets the pagination's pageIndex whenever the filter
 * is changed.
 *
 * For calendar-day filters over a `LocalDate` column, use `useDateFilterSearchParam`.
 *
 * @typedef T - The type of the object that contains the search parameter
 * @param key - The key of the search parameter to manage
 * @param prefix - Optional prefix for the URL keys
 */
export function useDateTimeFilterSearchParam<T extends Record<string, unknown>>(key: Extract<keyof T, string>, prefix?: string) {
  const [param, setParam] = useDateTimeSearchParam(key, prefix);
  const setFilter = useSetterWithPaginationReset(setParam, prefix);

  return [param, setFilter] as const;
}
