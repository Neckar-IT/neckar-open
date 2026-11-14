import { type UseQueryOptions } from "@tanstack/react-query";
import { useQuery, type QueryResult } from "./useQuery";

/**
 * Customized useQuery Hook for fetching dynamic data
 *
 * Features:
 * - fetches data in a regular interval (default: 1s)
 * - will not refresh the data on window focus or mount
 * - failed queries will not be retried, as the next query will be performed before the retry
 */
export function useQueryForDynamicData<TData>(options: UseQueryOptions<TData>): QueryResult<TData> {
  return useQuery<TData>({
    refetchInterval: 1000,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    staleTime: 0,
    retry: false,
    ...options,
  });
}
