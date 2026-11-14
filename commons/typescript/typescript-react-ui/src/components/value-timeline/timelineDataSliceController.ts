import { usePaginationSearchParams, useSortingSearchParams } from "@neckar-hub/typescript-react-utils/search-params";
import { PriceTimelineEntriesSortCriteria } from "@neckar-hub/typescript-utils/openapi-types";
import type { DataSliceController, DataSliceParams } from "../layout/data-slice-view";

/**
 * Pagination + sorting state for a value-timeline overview table.
 *
 * No user-facing filters today — the backend exposes a `validFromAfter` filter
 * for future use but the UI does not surface it. Sort defaults to descending
 * `validFrom` (newest first) on the backend when no `sortBy` is passed.
 *
 * Generic over the parent-specific query-params type so each consumer can pass
 * its own `Get*TimelineFor*Params` (with the parent id baked in) — only the
 * pagination + sorting fields are touched here.
 */
export type TimelineDataSliceController<TParams extends DataSliceParams<PriceTimelineEntriesSortCriteria>> = Pick<
  DataSliceController<TParams, PriceTimelineEntriesSortCriteria>,
  "params" | "setPagination" | "setSorting"
>;

export function useTimelineDataSliceControllerFromUrl<TParams extends DataSliceParams<PriceTimelineEntriesSortCriteria>>(
  prefix?: string,
): TimelineDataSliceController<TParams> {
  const [pagination, setPagination] = usePaginationSearchParams(undefined, prefix);
  const [sorting, setSorting] = useSortingSearchParams(PriceTimelineEntriesSortCriteria, prefix);

  return {
    params: {
      ...pagination,
      ...sorting,
    } as TParams,
    setPagination,
    setSorting,
  };
}
