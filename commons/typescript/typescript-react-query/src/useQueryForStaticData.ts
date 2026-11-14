import { type UseQueryOptions } from "@tanstack/react-query";
import { useQuery, type QueryResult } from "./useQuery";

/**
 * Customized useQuery hook for fetching static data
 *
 * This hook can be used to query for static data, i.e. data that does not need regular invalidations.
 * Features:
 * - fetches data once
 * - refresh on window focus and mount
 */
export function useQueryForStaticData<TData>(options: UseQueryOptions<TData>): QueryResult<TData> {
  return useQuery<TData>({
    refetchOnMount: true,
    refetchOnWindowFocus: true,
    ...options,
  });
}
