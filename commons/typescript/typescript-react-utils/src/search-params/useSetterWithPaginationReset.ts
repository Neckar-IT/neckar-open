/**
 * Imports
 */

import { usePaginationSearchParams } from "./usePaginationSearchParams";

/**
 * Wraps the setter of a search parameter so that every change also resets the pagination's
 * pageIndex. Shared by all `use*FilterSearchParam` hooks: a filtered table or list must start
 * at the first page again, otherwise the user stays on a page the new result set may not have.
 *
 * @param setParam - The setter of the wrapped search parameter
 * @param prefix - Optional prefix for the URL keys, the same one the wrapped parameter uses
 */

export function useSetterWithPaginationReset<T>(setParam: (value: T | undefined) => void, prefix?: string) {
  const [, , resetPageIndex] = usePaginationSearchParams(undefined, prefix);

  return (value: T | undefined) => {
    setParam(value);
    resetPageIndex();
  };
}
