import { type UseQueryOptions } from "@tanstack/react-query";
import { useQuery, type QueryResult } from "./useQuery";

/**
 * Hook to query for table data with automatic background refetch.
 *
 * Configures the query for table use cases:
 * - 30-second auto-refetch interval
 * - Refetch on mount and window focus
 * - No stale time (always refetch)
 *
 * When query parameters change (filter, pagination), the query transitions to
 * PENDING status, allowing the UI to show appropriate loading states (e.g., skeletons).
 */
export function useQueryForTableData<TData>(options: UseQueryOptions<TData>): QueryResult<TData> {
  return useQuery<TData>({
    refetchInterval: 30000,
    refetchOnMount: true,
    refetchOnWindowFocus: true,
    staleTime: 0,
    ...options,
  });
}
