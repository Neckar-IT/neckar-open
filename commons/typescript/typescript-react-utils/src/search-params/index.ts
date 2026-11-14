/**
 * Re-Export the provider to use with react-router
 *
 * This provider needs to wrap the RouterProvider from react-router
 */
export { NuqsAdapter as SearchParamsProvider } from "nuqs/adapters/react-router/v7";

/**
 * Re-Export the testing provider for use in unit tests.
 *
 * Supplies search-param state without a react-router context, so components that read
 * search params can be rendered in isolation. Pass the initial params via the
 * `searchParams` prop (e.g. `searchParams="?archiveFilter=Archived"`).
 */
export { NuqsTestingAdapter as SearchParamsTestingProvider } from "nuqs/adapters/testing";

export { DEFAULT_PAGE_SIZE, type OnPaginationParamsChange, type PaginationParams } from "./paginationParams";
export { usePaginationSearchParams } from "./usePaginationSearchParams";
export { useSortingSearchParams, type OnSortingParamsChange, type SortingParams } from "./useSortingSearchParams";

export { useStringFilterSearchParam } from "./useStringFilterSearchParam";
export { useStringSearchParam } from "./useStringSearchParam";

export { useIntegerFilterSearchParam } from "./useIntegerFilterSearchParam";
export { useIntegerSearchParam } from "./useIntegerSearchParam";

export { useFloatFilterSearchParam } from "./useFloatFilterSearchParam";
export { useFloatSearchParam } from "./useFloatSearchParam";

export { useBooleanFilterSearchParam } from "./useBooleanFilterSearchParam";
export { useBooleanSearchParam } from "./useBooleanSearchParam";

export { useDateFilterSearchParam } from "./useDateFilterSearchParam";
export { useDateSearchParam } from "./useDateSearchParam";

export { useDateTimeFilterSearchParam } from "./useDateTimeFilterSearchParam";
export { useDateTimeSearchParam } from "./useDateTimeSearchParam";

export { useEnumFilterSearchParam } from "./useEnumFilterSearchParam";
export { useEnumSearchParam } from "./useEnumSearchParam";
