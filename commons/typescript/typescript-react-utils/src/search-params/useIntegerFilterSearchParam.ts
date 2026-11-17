/**
 * Imports
 */

import { useIntegerSearchParam } from "./useIntegerSearchParam";
import { useSetterWithPaginationReset } from "./useSetterWithPaginationReset";

/**
 * Hook to manage a single integer search parameter that is use as a filter for tables or lists.
 * This hook will reset the pagination's pageIndex whenever the filter is changed.
 *
 * @typedef T - The type of the object that contains the search parameter
 * @param key - The key of the search parameter to manage
 * @param defaultValue - Optional default value for the search parameter
 * @param prefix - Optional prefix for the URL keys
 */

export function useIntegerFilterSearchParam<T extends Record<string, unknown>>(
  key: Extract<keyof T, string>,
  defaultValue?: number,
  prefix?: string,
) {
  const [param, setParam] = useIntegerSearchParam(key, defaultValue, prefix);
  const setFilter = useSetterWithPaginationReset(setParam, prefix);

  return [param, setFilter] as const;
}
