/**
 * Imports
 */

import { useDateSearchParam } from "./useDateSearchParam";
import { useSetterWithPaginationReset } from "./useSetterWithPaginationReset";

/**
 * Calendar-date filter for `LocalDate` columns (rare — see ADR 0150). For the normal
 * `Instant` case, use `useDateTimeFilterSearchParam`. Wire format and semantics match
 * `useDateSearchParam`; resets `pageIndex` on change.
 */

export function useDateFilterSearchParam<T extends Record<string, unknown>>(key: Extract<keyof T, string>, prefix?: string) {
  const [param, setParam] = useDateSearchParam(key, prefix);
  const setFilter = useSetterWithPaginationReset(setParam, prefix);

  return [param, setFilter] as const;
}
