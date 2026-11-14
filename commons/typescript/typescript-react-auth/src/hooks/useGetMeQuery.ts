/**
 * Imports
 */

import { useQueryPrimitive } from "@neckar-hub/typescript-react-query";
import type { MeResponse } from "@neckar-hub/typescript-utils/openapi-types";

/**
 * Query Key
 */

export const getMeQueryKey = ["getMe"];

/**
 * Result Type
 *
 * The `/me` query is the single place in the auth stack that must observe error
 * responses directly (401 triggers auto-login / invalid-token flows). It therefore
 * bypasses the standard `QueryResult` wrapper — which is intentionally error-free —
 * and exposes a tailored union including an explicit `error` variant.
 */
export type GetMeResult =
  | { status: "pending" }
  /** [response] is the session loaded before, when a refetch failed. */
  | { status: "error"; error: unknown; response: MeResponse | undefined }
  | { status: "success"; response: MeResponse };

/**
 * Query Hook
 *
 * Uses `useQuery` from `@tanstack/react-query` directly (rather than the project's
 * slim `useQuery` wrapper) because the auth provider needs to react to error
 * responses in code — they must not be thrown to the error boundary.
 */
export function useGetMeQuery(onGetMe: () => Promise<MeResponse>): GetMeResult {
  const result = useQueryPrimitive({
    queryKey: getMeQueryKey,
    queryFn: onGetMe,

    // Errors are handled by the AuthProvider components (not thrown to the error boundary)
    throwOnError: false,

    // Prevent refetch on window focus to avoid rerendering the CredentialsAuthProvider login screen
    // and losing data the user might have entered
    refetchOnWindowFocus: false,

    meta: {
      // there is no need to invalidate this query automatically after each mutation as the user data will not change
      // during the session
      preventBeingResetByMutations: true,
    },
  });

  switch (result.status) {
    case "pending":
      return { status: "pending" };
    case "error":
      return { status: "error", error: result.error, response: result.data };
    case "success":
      return { status: "success", response: result.data };
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(result)}`);
  }
}
