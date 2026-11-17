/**
 * Imports
 */

import type { InvalidateOptions, Query, QueryClient } from "@tanstack/react-query";

/**
 * Whether a successful mutation reloads [query]: every query except those marked with `meta.preventBeingResetByMutations = true`.
 * The single place that evaluates the flag, so a flagged query such as `useGetMeQuery` survives every reload below.
 */
function isReloadedByMutations(query: Query): boolean {
  return !query.meta?.preventBeingResetByMutations;
}

/**
 * Resets every query a successful mutation reloads; the global mutation reset in `createQueryClient` and every
 * explicit reset (e.g. reloading after a version conflict) call it.
 */
export function resetQueriesExceptPrevented(queryClient: QueryClient): Promise<void> {
  return queryClient.resetQueries({ predicate: isReloadedByMutations });
}

/**
 * Fetches every query a successful mutation reloads again and keeps its data on screen meanwhile, so a form below a
 * query stays mounted.
 */
export function refetchQueriesExceptPrevented(queryClient: QueryClient, options?: InvalidateOptions): Promise<void> {
  return queryClient.invalidateQueries({ predicate: isReloadedByMutations }, options);
}
