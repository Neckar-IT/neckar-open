import { isServiceUnavailable, type QueryClient, reactionToFailure, useQueryClient } from "@neckar-hub/typescript-react-query";
import { ActionLockProvider } from "@neckar-hub/typescript-react-ui/actions";
import { mutationFailedToastId, toast } from "@neckar-hub/typescript-react-ui/feedback";
import type { HealthResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { useEffect, useRef } from "react";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { AppConnectivityMessage } from "./AppConnectivityMessage";
import {
  DefaultHealthPollIntervalMs,
  HealthCheckQueryKey,
  isHealthCheckQuery,
  pollStateToServiceAvailability,
  type ServiceAvailability,
  useDeviceIsOnline,
  useLastPollAnswerAt,
  useStartRecoveryRound,
  useDeviceOnlineListener,
  useServiceAvailability,
  useServiceHealthPoll,
} from "./appConnectivityState";
import { connectivityNotice, serviceAvailabilityText } from "./serviceAvailabilityText";
import { resetUnavailableQueries, ServiceUnavailableBoundary } from "./ServiceUnavailableBoundary";

interface AppConnectivityProviderProps {
  /** Performs `GET /api/<product>/health`, which answers 200 in every state of the service. */
  healthCheckFn: () => Promise<HealthResponse>;
  children: React.ReactNode;
  mockDeviceOffline?: boolean;
  /** Fixed availability instead of the poll, for stories and tests. */
  mockServiceAvailability?: ServiceAvailability;
  refetchInterval?: number;
}

/**
 * Tracks whether the device is online and the service available, shows a banner otherwise and locks every action below.
 * Renders `ServiceUnavailablePage` for a query without data of an unavailable service and names the health state in the toast of a mutation the service did not run.
 * Uses react-query for the health poll, so it has to be used inside a QueryClientProvider!
 */
export function AppConnectivityProvider(props: AppConnectivityProviderProps): React.ReactElement {
  useDeviceOnlineListener(props.mockDeviceOffline ?? false);
  useServiceHealthPoll(props.healthCheckFn, props.refetchInterval, props.mockServiceAvailability);
  useUnavailableServiceReactions(props.refetchInterval ?? DefaultHealthPollIntervalMs);

  const lockReason = useConnectivityLockReason();

  return (
    <>
      <AppConnectivityMessage />
      <ActionLockProvider reason={lockReason}>
        <ServiceUnavailableBoundary>{props.children}</ServiceUnavailableBoundary>
      </ActionLockProvider>
    </>
  );
}

function useConnectivityLockReason(): string | undefined {
  const { t } = useEmbeddedTranslation();
  const notice = connectivityNotice(useDeviceIsOnline(), useServiceAvailability());
  return notice === undefined || notice.kind === "hint" ? undefined : t(notice.textKey);
}

/**
 * Polls at once after a request of an unavailable service, names the health state in the toast of a mutation the service did not run,
 * and fetches failed queries again once the service is available.
 */
function useUnavailableServiceReactions(recoveryIntervalMs: number): void {
  const { t } = useEmbeddedTranslation();
  const queryClient = useQueryClient();
  const availability = useServiceAvailability();

  const lastPollAnswerAt = useLastPollAnswerAt();
  const startRecoveryRound = useStartRecoveryRound();
  const lastRecoveryAt = useRef(0);
  // Every poll answering `available` fetches the queries again that failed meanwhile, also after a blip the poll never saw.
  // About one round per poll interval: a route that keeps failing while the poll answers `available` triggers a poll each time.
  // Half the interval as the gap keeps a regular poll answer arriving a little early from skipping its round.
  useEffect(() => {
    if (availability?.type !== "available" || Date.now() - lastRecoveryAt.current < recoveryIntervalMs / 2) {
      return;
    }
    lastRecoveryAt.current = Date.now();
    void resetUnavailableQueries(queryClient);
    startRecoveryRound();
  }, [availability?.type, lastPollAnswerAt, queryClient, recoveryIntervalMs, startRecoveryRound]);

  useEffect(() => {
    return queryClient.getQueryCache().subscribe((event) => {
      // `failed` reports every failed attempt, `error` only the last after the retries.
      if (
        event.type !== "updated" ||
        (event.action.type !== "failed" && event.action.type !== "error") ||
        isHealthCheckQuery(event.query.queryKey)
      ) {
        return;
      }
      if (isServiceUnavailable(event.action.error)) {
        void pollNow(queryClient);
      }
    });
  }, [queryClient]);

  useEffect(() => {
    // A newer mutation result owns the toast; a replacement arriving after it stays away.
    let latestFailure = 0;
    const toastNotExecuted = async () => {
      const failure = ++latestFailure;
      await pollNow(queryClient);
      if (failure !== latestFailure) {
        return;
      }
      const polled = polledAvailability(queryClient);
      // A service the poll reports available, degraded included, names no reason the mutation did not run.
      const text = polled?.type === "available" ? undefined : serviceAvailabilityText(polled ?? { type: "unreachable" });
      const message =
        text === undefined
          ? t("mutationNotExecuted.withoutReason")
          : t({ key: "mutationNotExecuted.withReason", options: { reason: t(text.textKey) } });
      toast.error(message, { id: mutationFailedToastId });
    };

    return queryClient.getMutationCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "success") {
        latestFailure++;
      }
      if (event.type !== "updated" || event.action.type !== "error") {
        return;
      }

      const reaction = reactionToFailure(event.action.error, "mutation");
      // A cancelled mutation shows nothing, so it leaves the toast to the failure before it.
      if (reaction.type !== "notExecuted" && reaction.type !== "cancelled") {
        latestFailure++;
      }
      switch (reaction.type) {
        case "notExecuted":
          // createMutationErrorToastHandler toasts it first; the health state names why the service did not run it.
          void toastNotExecuted();
          return;
        case "unknownOutcome":
          void pollNow(queryClient);
          return;
        case "cancelled":
        case "sessionExpired":
        case "permissionDenied":
        case "versionMismatch":
        case "errorPage":
          return;
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(reaction)}`);
      }
    });
  }, [queryClient, t]);
}

function pollNow(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({ queryKey: HealthCheckQueryKey }, { cancelRefetch: false });
}

/**
 * The availability the latest answer of the health poll stands for.
 */
function polledAvailability(queryClient: QueryClient): ServiceAvailability | undefined {
  const state = queryClient.getQueryState<HealthResponse>(HealthCheckQueryKey);
  return state === undefined ? undefined : pollStateToServiceAvailability(state);
}
