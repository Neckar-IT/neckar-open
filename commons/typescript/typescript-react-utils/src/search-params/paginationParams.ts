/**
 * The pagination contract, in a module that imports nothing: `usePaginationState` reads the
 * defaults from here, so the `utility-hooks` barrel stays free of `nuqs`.
 */

export interface PaginationParams {
  pageIndex: number;
  pageSize: number;
}

export type OnPaginationParamsChange = (value: PaginationParams | undefined | ((old: PaginationParams) => PaginationParams)) => void;

export const DEFAULT_PAGE_INDEX = 0;
export const DEFAULT_PAGE_SIZE = 10;
