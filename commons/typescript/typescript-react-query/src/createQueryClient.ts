/**
 * Imports
 */

import { isBadResponse } from "@neckar-hub/typescript-utils/type-guards";
import { MutationCache, QueryClient } from "@tanstack/react-query";
import { type FailureReaction, reactionToFailure } from "./reactionToFailure";
import { resetQueriesExceptPrevented } from "./resetQueriesExceptPrevented";

export interface CreateQueryClientOptions {
  /**
   * Called for **every** failed mutation, at the `MutationCache` level. Unlike per-call handling
   * (`mutateAsync().catch(...)`, `throwOnError` render-throws), this fires even when the caller
   * discards the returned promise (`void form.handleSubmit(async ...)()`), so a mutation failure
   * can never disappear silently. A cancelled request is no failure and is not reported. Typical wiring: report to OTel
   * (`captureError(error, "mutation")`) and show an error toast.
   */
  onMutationError?: (error: unknown) => void;

  /**
   * A same-origin HTTP 401 ends the session: no retry, no render-throw, no `onMutationError`. Set it
   * under `ProxyAuthProvider`, which shows the sign-in-again screen for that 401 instead.
   */
  unauthorizedEndsSession?: boolean;
}

/**
 * QueryClient factory function.
 *
 * @returns {QueryClient} An instance of the QueryClient with an opinionated default configuration.
 */
export function createQueryClient(options: CreateQueryClientOptions = {}): QueryClient {
  const endsSession = (reaction: FailureReaction): boolean =>
    options.unauthorizedEndsSession === true && reaction.type === "sessionExpired";
  // Whether a reaction replaces the screen; the others keep the data, the form, or a toast and alert of the mutation error handler.
  const rendersErrorPage = (reaction: FailureReaction): boolean => {
    switch (reaction.type) {
      case "serviceUnavailablePage":
      case "permissionDenied":
      case "versionMismatch":
      case "errorPage":
        return true;
      case "sessionExpired":
        return !endsSession(reaction);
      case "cancelled":
      case "keepDataWithBanner":
      case "notExecuted":
      case "unknownOutcome":
        return false;
      default:
        throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(reaction)}`);
    }
  };

  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: {
        throwOnError: (error) => rendersErrorPage(reactionToFailure(error, "mutation")),
      },
      queries: {
        /**
         * How often a query will retry before it is marked as failed.
         * The default is 3, which means it will take at least 7 seconds before a query is marked as failed.
         * (Query => 1 sec => 1. Retry => 2 sec => 2. Retry => 4 sec => 3. Retry => Query Failed)
         *
         * We set the retry count to 2, so queries will fail faster (after approximately 3 seconds).
         */
        retry: (failureCount, error) => !endsSession(reactionToFailure(error, "query")) && failureCount < 2,
        throwOnError: (error, query) =>
          rendersErrorPage(reactionToFailure(error, query.state.data === undefined ? "query" : "queryWithData")),
      },
    },
    mutationCache: new MutationCache({
      /**
       * Surface every mutation failure — see [CreateQueryClientOptions.onMutationError].
       */
      onError: (error) => {
        const reaction = reactionToFailure(error, "mutation");
        if (reaction.type === "cancelled" || endsSession(reaction)) return;
        options.onMutationError?.(error);
      },
      /**
       * Resets all queries when a mutation finishes *successfully*.
       * Since we are using the `onSettled` callback here, the per mutation `onSuccess` and `onError` callbacks will
       * run first.
       *
       * "Success" means:
       *   - The mutation promise resolved (did not throw).
       *   - If the resolved value is a discriminated-union `RestResponse` (object with a string `type` field),
       *     the `type` must not start with `"bad/"` — `bad/*` responses represent domain failures that should
       *     NOT wipe the query cache, because the calling UI typically relies on the cache to keep the page
       *     stable while it surfaces the error (e.g. `form.setError` on the offending field).
       *     A `bad/*` type listed in `meta.resetQueriesOnBadResponses` of the mutation counts as success.
       *   - Other shapes (void, primitives, objects without a `type` discriminator) are treated as success.
       *
       * You can opt out of the automatic query reset by setting `meta.skipGlobalQueryResets = true` on the
       * mutation, or prevent a specific query from being reset by mutations via
       * `meta.preventBeingResetByMutations = true` on the query.
       */
      onSettled: (data, error, _variables, _context, mutation) => {
        if (mutation.meta?.skipGlobalQueryResets) return;
        if (error != null) return;
        if (isBadResponse(data) && mutation.meta?.resetQueriesOnBadResponses?.includes(data.type) !== true) return;

        void resetQueriesExceptPrevented(queryClient);
      },
    }),
  });

  return queryClient;
}
