/**
 * Imports
 */

import { useBooleanSearchParam } from "./useBooleanSearchParam";
import { useSetterWithPaginationReset } from "./useSetterWithPaginationReset";

/**
 * Hook to manage a single string search parameter that is use as a filter for tables or lists.
 * This hook will reset the pagination's pageIndex whenever the filter is changed.
 *
 * @typedef T - The type of the object that contains the search parameter
 * @param key - The key of the search parameter to manage
 * @param defaultValue - Optional default value for the search parameter
 * @param prefix - Optional prefix for the URL keys
 */

export function useBooleanFilterSearchParam<T extends Record<string, unknown>>(
  key: Extract<keyof T, string>,
  defaultValue: boolean = false,
  prefix?: string,
) {
  const [param, setParam] = useBooleanSearchParam(key, defaultValue, prefix);
  const setFilter = useSetterWithPaginationReset(setParam, prefix);

  return [param, setFilter] as const;
}
