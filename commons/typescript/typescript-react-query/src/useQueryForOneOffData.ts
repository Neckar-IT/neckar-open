import { type UseQueryOptions } from "@tanstack/react-query";
import { useQuery, type QueryResult } from "./useQuery";

/**
 * Custom Hook for loading one-off data (i.e. data that is needed only once).
 *
 * Features:
 * - fetches data once on mount
 * - does not cache the response after the hook is unmounted
 * - no refetch on window focus or reconnect
 */
export function useQueryForOneOffData<TData>(options: UseQueryOptions<TData>): QueryResult<TData> {
  return useQuery<TData>({
    gcTime: 0,
    staleTime: 0,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    ...options,
  });
}
