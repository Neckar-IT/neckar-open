import type { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import type { OnPaginationParamsChange } from "../search-params/paginationParams";
import type { OnSortingParamsChange } from "../search-params/useSortingSearchParams";

/**
 * The pagination and sorting parameters every table request carries; the generated `Params` add the filters.
 */
export interface DataSliceParams<SortCriteria> {
  pageIndex?: number;
  pageSize?: number;

  sortBy?: SortCriteria;
  sortOrder?: SortOrder;
}

/**
 * The request parameters of a table that are neither pagination nor sorting: the filters.
 */
export type FilterParams<Params> = Omit<Params, keyof DataSliceParams<unknown>>;

/**
 * The sort criteria of a table, as its generated `Params` name them.
 */
export type SortCriteriaOf<Params extends DataSliceParams<string>> = NonNullable<Params["sortBy"]>;

/**
 * The state of a table: its request parameters and the setters of each. `useDataSliceControllerFromUrl` keeps it in
 * the URL, `useDataSliceControllerFromState` in the component.
 */
export interface DataSliceController<Params extends DataSliceParams<SortCriteria>, SortCriteria> {
  /** The request parameters: pagination, sorting and one value per filter parameter of `Params`. */
  params: Params;
  /** The tokens of the parameter `freeText`, for `HighlightSource`. */
  freeTextTokens: string[] | undefined;

  /** Sets one filter parameter and goes back to the first page. */
  setParam<Key extends keyof FilterParams<Params>>(key: Key, value: FilterParams<Params>[Key]): void;
  /** Sets several filter parameters in one step and goes back to the first page, e.g. both ends of a range. */
  setParams(values: Partial<FilterParams<Params>>): void;
  setPagination: OnPaginationParamsChange;
  setSorting: OnSortingParamsChange<SortCriteria>;
}
