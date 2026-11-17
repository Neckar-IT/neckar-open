import { keepPreviousData, type UseQueryOptions } from "@tanstack/react-query";
import { useQuery, type QueryResult } from "./useQuery";

/**
 * Hook to query for search results whose query key contains the search text.
 *
 * Configures the query for type-ahead use cases:
 * - keeps the results of the previous search text while the new one is loading
 * - refetch on mount and window focus
 * - no stale time (always refetch)
 *
 * The kept results turn the status transition on a changed search text from PENDING into
 * FETCHING. The result list therefore never disappears while typing, and components that
 * gate rendering on PENDING (`QueriesResolved`) keep their children — and their React
 * state — mounted.
 *
 * Use `useQueryForTableData` for tables instead: those show a skeleton while loading and
 * rely on the PENDING transition to do so.
 */
export function useQueryForSearchResults<TData>(options: UseQueryOptions<TData>): QueryResult<TData> {
  return useQuery<TData>({
    placeholderData: keepPreviousData,
    refetchOnMount: true,
    refetchOnWindowFocus: true,
    staleTime: 0,
    ...options,
  });
}
