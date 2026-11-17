/**
 * Imports
 */

import { useSetterWithPaginationReset } from "./useSetterWithPaginationReset";
import { useStringSearchParam } from "./useStringSearchParam";

/**
 * Hook to manage a single string search parameter that is use as a filter for tables or lists.
 * This hook will reset the pagination's pageIndex whenever the filter is changed.
 *
 * @typedef T - The type of the object that contains the search parameter
 * @param key - The key of the search parameter to manage
 * @param prefix - Optional prefix for the URL keys
 */

export function useStringFilterSearchParam<T extends Record<string, unknown>>(key: Extract<keyof T, string>, prefix?: string) {
  const [param, setParam] = useStringSearchParam(key, prefix);
  const setFilter = useSetterWithPaginationReset(setParam, prefix);

  return [param, setFilter] as const;
}
