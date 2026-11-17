/**
 * Imports
 */

import { useEnumSearchParam } from "./useEnumSearchParam";
import { useSetterWithPaginationReset } from "./useSetterWithPaginationReset";

/**
 * Hook to manage a single enum search parameter that is used as a filter for tables or lists.
 * This hook will reset the pagination's pageIndex whenever the filter is changed.
 *
 * @typedef T - The type of the object that contains the search parameter
 * @param enumValues - The const object containing all enum values (e.g., `ConfirmationStatus`)
 * @param key - The key of the search parameter to manage
 * @param prefix - Optional prefix for the URL keys
 */

export function useEnumFilterSearchParam<T extends Record<string, unknown>, E extends Record<string, string>>(
  enumValues: E,
  key: Extract<keyof T, string>,
  prefix?: string,
) {
  const [param, setParam] = useEnumSearchParam(enumValues, key, undefined, prefix);
  const setFilter = useSetterWithPaginationReset(setParam, prefix);

  return [param, setFilter] as const;
}
