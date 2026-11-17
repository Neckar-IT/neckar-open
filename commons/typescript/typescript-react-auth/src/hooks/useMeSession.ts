import { type OutageFailure, reactionToFailure, useQueryClient } from "@neckar-hub/typescript-react-query";
import type { MeResponse, UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { isAxiosError } from "axios";
import { useEffect, useMemo, useRef } from "react";
import { type GetMeResult, getMeQueryKey, useGetMeQuery } from "./useGetMeQuery";

/**
 * What `GET /me` says about the current session — the same outcomes whether the token was
 * obtained by keycloak-js or forwarded by the OIDC middleware at the edge, because both providers
 * ask the backend the same question.
 */
export type MeSession =
  | { type: "loading" }
  /** The service is out of reach or not ready. Retried on an interval. */
  | { type: "serviceUnavailable"; failure: OutageFailure }
  /** Any other failure; `MeSessionScreens` throws [error] to the error boundary, which renders `UnexpectedErrorPage`, `PermissionDeniedPage` or the version mismatch page. */
  | { type: "failed"; error: unknown }
  /** The backend of the page's own origin answered 401: the token is gone, expired or not accepted. */
  | { type: "sessionInvalid" }
  /** Authenticated, but this service holds no account for the identity — HTTP 200. */
  | { type: "notRegistered"; selfSignupAllowed: boolean }
  | { type: "registered"; user: UserDetails };

/**
 * Polls while the backend is unreachable or not ready, on top of React Query's own two retries:
 * those cover a transient failure within seconds, this one covers an outage lasting minutes.
 */
const RetryIntervalMs = 5_000;

export function useMeSession(onGetMe: () => Promise<MeResponse>): MeSession {
  const queryClient = useQueryClient();

  // Diagnostic start/done logs around the product's own call, so the browser console shows when
  // the request started and how long it took — the phase that dominates a slow reload.
  const instrumentedGetMe = useMemo(
    () => async () => {
      const startedAt = performance.now();
      console.info("[Auth] GET /me: start");
      try {
        const response = await onGetMe();
        console.info(`[Auth] GET /me: done in ${Math.round(performance.now() - startedAt)}ms`);
        return response;
      } catch (error: unknown) {
        const httpStatus = isAxiosError(error) ? error.response?.status : undefined;
        const suffix = httpStatus === undefined ? "" : ` (HTTP ${httpStatus})`;
        console.error(`[Auth] GET /me: failed after ${Math.round(performance.now() - startedAt)}ms${suffix}`, error);
        throw error;
      }
    },
    [onGetMe],
  );

  const fetchedSession = toMeSession(useGetMeQuery(instrumentedGetMe));
  // A refetch without a loaded session passes through pending; the outage stays on screen until /me answers.
  const lastOutage = useRef<Extract<MeSession, { type: "serviceUnavailable" }> | undefined>(undefined);
  if (fetchedSession.type === "serviceUnavailable") {
    lastOutage.current = fetchedSession;
  } else if (fetchedSession.type !== "loading") {
    lastOutage.current = undefined;
  }
  const session = fetchedSession.type === "loading" && lastOutage.current !== undefined ? lastOutage.current : fetchedSession;
  const shouldRetry = session.type === "serviceUnavailable";

  useEffect(() => {
    if (!shouldRetry) {
      return;
    }

    const intervalId = setInterval(() => {
      void queryClient.invalidateQueries({ queryKey: getMeQueryKey });
    }, RetryIntervalMs);

    return () => clearInterval(intervalId);
  }, [shouldRetry, queryClient]);

  return session;
}

function toMeSession(result: GetMeResult): MeSession {
  switch (result.status) {
    case "pending":
      return { type: "loading" };

    case "error": {
      // onGetMe is the product's own function and may reject with anything, a thrown TypeError as readily as a failed request.
      const reaction =
        result.response === undefined ? reactionToFailure(result.error, "query") : reactionToFailure(result.error, "queryWithData");
      switch (reaction.type) {
        case "keepDataWithBanner":
          // A refetch of a loaded session failed during an outage: the session stays; the banner of AppConnectivityProvider names the outage.
          return result.response === undefined ? { type: "loading" } : toMeSession({ status: "success", response: result.response });
        case "serviceUnavailablePage":
          return { type: "serviceUnavailable", failure: reaction.failure };
        case "sessionExpired":
          return { type: "sessionInvalid" };
        case "permissionDenied":
        case "versionMismatch":
        case "errorPage":
          return { type: "failed", error: result.error };
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(reaction)}`);
      }
    }

    case "success": {
      const response = result.response;
      switch (response.type) {
        case "good":
          return { type: "registered", user: response.data };
        case "bad/NotRegistered":
          return { type: "notRegistered", selfSignupAllowed: response.selfSignupAllowed };
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(response)}`);
      }
    }

    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(result)}`);
  }
}
