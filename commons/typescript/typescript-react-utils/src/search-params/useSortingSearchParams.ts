/**
 * Imports
 */

import { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import { parseAsStringEnum, useQueryStates } from "nuqs";
import { usePaginationSearchParams } from "./usePaginationSearchParams";

/**
 * Type Declarations
 */

export interface SortingParams<SortCriteria> {
  sortBy: SortCriteria;
  sortOrder: SortOrder;
}

export type OnSortingParamsChange<SortCriteria> = (value: SortingParams<SortCriteria> | undefined) => void;

/**
 * Hook to manage search parameters for sorting.
 * This hook will reset the pageIndex to 0 whenever the sorting changes.
 *
 * @param sortByOptions - An object containing the possible values for the sortBy parameter
 * @param prefix - Optional prefix for the URL keys
 *
 * @returns A tuple containing the current sorting parameters and a function to update them. The sorting parameters will
 *          either both be defined or the object is undefined.
 */

export function useSortingSearchParams<SortCriteria extends Record<string, string>>(sortByOptions: SortCriteria, prefix?: string) {
  const [, , resetPageIndex] = usePaginationSearchParams(undefined, prefix);

  const [param, setParam] = useQueryStates(
    {
      sortBy: parseAsStringEnum<SortCriteria[keyof SortCriteria]>(Object.values(sortByOptions) as SortCriteria[keyof SortCriteria][]),
      sortOrder: parseAsStringEnum<SortOrder>(Object.values(SortOrder)),
    },
    {
      history: "push",
      urlKeys: {
        sortBy: prefix ? `${prefix}SortBy` : "sortBy",
        sortOrder: prefix ? `${prefix}SortOrder` : "sortOrder",
      },
    },
  );

  const setSorting: OnSortingParamsChange<SortCriteria[keyof SortCriteria]> = (value) => {
    if (value === undefined) {
      if (param.sortBy === null && param.sortOrder === null) {
        return;
      }

      void setParam(null); // reset to defaults
    } else {
      if (value.sortBy === param.sortBy && value.sortOrder === param.sortOrder) {
        return;
      }

      void setParam(value);
    }

    resetPageIndex();
  };

  const sortingValue: SortingParams<SortCriteria[keyof SortCriteria]> | undefined =
    param.sortBy !== null && param.sortOrder !== null
      ? {
          sortBy: param.sortBy,
          sortOrder: param.sortOrder,
        }
      : undefined;

  return [sortingValue, setSorting] as const;
}
