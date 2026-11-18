/**
 * Re-exports from @tanstack/react-query
 */
export {
  keepPreviousData,
  QueryClientContext,
  QueryClientProvider,
  QueryClient,
  queryOptions,
  skipToken,
  useMutation,
  useQueryClient,
  useSuspenseQuery,
  type Query,
  type QueryKey,
  type UseMutateAsyncFunction,
  type UseMutationResult,
  type UseSuspenseQueryOptions,
} from "@tanstack/react-query";

/**
 * Factory function to create a query client
 */
export { createQueryClient, type CreateQueryClientOptions } from "./createQueryClient";

/**
 * HTTP 401 of the page's own origin
 */
export { isSameOriginUnauthorized } from "./isSameOriginUnauthorized";

/**
 * Classification of a failed request by what it says about the service
 */
export { classifyHttpFailure, isServiceUnavailable } from "./classifyHttpFailure";

/**
 * The one decision what the application shows for a failed request
 */
export {
  reactionToFailure,
  type FailureReaction,
  type MutationFailureReaction,
  type OutageFailure,
  type QueryFailureReaction,
  type QueryWithDataFailureReaction,
  type RequestKind,
} from "./reactionToFailure";

/**
 * Test factory: a query client that starts no garbage-collection timer
 */
export { createQueryClientWithoutGcTimer } from "./createQueryClientWithoutGcTimer";

/**
 * Reset that respects `meta.preventBeingResetByMutations`
 */
export { refetchQueriesExceptPrevented, resetQueriesExceptPrevented } from "./resetQueriesExceptPrevented";

/**
 * Query Client Provider with Devtools
 */
export { QueryClientProviderWithDevtools } from "./QueryClientProviderWithDevtools";

/**
 * Custom query hooks
 */
export { QueryStatus, useQuery, type QueryResult, type QueryResultSuccess } from "./useQuery";

/**
 * Escape hatch: raw `useQuery` from `@tanstack/react-query`.
 *
 * Only for library hooks that must inspect errors directly (e.g. `useGetMeQuery`
 * with `throwOnError: false`). App code should use the wrapper `useQuery` above.
 */
export { useQuery as useQueryPrimitive } from "@tanstack/react-query";
export { useQueryForDynamicData } from "./useQueryForDynamicData";
export { useQueryForOneOffData } from "./useQueryForOneOffData";
export { useQueryForSearchResults } from "./useQueryForSearchResults";
export { useQueryForStaticData } from "./useQueryForStaticData";
export { useQueryForTableData } from "./useQueryForTableData";

/**
 * Mutation for a single file upload with progress
 */
export {
  useFileUploadMutation,
  type FileUploadFunction,
  type UseFileUploadMutationOptions,
  type UploadProgress,
  type UseFileUploadMutationResult,
} from "./useFileUploadMutation";

/**
 * Module Augmentation for mutation meta
 */
import "./module-augmentation";
