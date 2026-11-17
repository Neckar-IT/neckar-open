import {
  classifyHttpFailure,
  type OutageFailure,
  reactionToFailure,
  useQueryClient,
  useQueryPrimitive,
} from "@neckar-hub/typescript-react-query";
import type { HealthResponse, ServiceStateDto } from "@neckar-hub/typescript-utils/openapi-types";
import { isAxiosError } from "axios";
import { atom, useAtomValueRawSync, useSetAtom } from "jotai";
import { useCallback, useEffect } from "react";

/**
 * What the health poll says about the service: answering and usable, answering and not ready, or out of reach.
 */
export type ServiceAvailability =
  | { type: "available"; state: AvailableServiceState }
  | { type: "notReady"; state: Exclude<ServiceStateDto, AvailableServiceState> }
  | { type: "unreachable" }
  | { type: "noConnection" };

type AvailableServiceState = Extract<ServiceStateDto, "ready" | "degraded">;

/**
 * Query key of the health poll; invalidating it polls at once.
 */
export const HealthCheckQueryKey = ["health-check"] as const;

export function isHealthCheckQuery(queryKey: readonly unknown[]): boolean {
  return queryKey[0] === HealthCheckQueryKey[0];
}

const deviceOnlineAtom = atom<boolean>(typeof window.navigator !== "undefined" ? window.navigator.onLine : true);

/**
 * Undefined until the first poll answers.
 */
const serviceAvailabilityAtom = atom<ServiceAvailability | undefined>(undefined);

const lastPollAnswerAtAtom = atom<number | undefined>(undefined);

/**
 * Counts the rounds in which `AppConnectivityProvider` fetched the failed queries again; `ServiceUnavailablePage` recovers on each new round.
 */
const recoveryRoundAtom = atom<number>(0);

export const DefaultHealthPollIntervalMs = 5000;

/**
 * The availability a health poll answer stands for.
 */
export function toServiceAvailability(response: Pick<HealthResponse, "state">): ServiceAvailability {
  switch (response.state) {
    case "ready":
    case "degraded":
      return { type: "available", state: response.state };
    case "starting":
    case "migrating":
    case "dependency-unavailable":
    case "schema-incompatible":
    case "migration-failed":
    case "fatal":
      return { type: "notReady", state: response.state };
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(response.state)}`);
  }
}

/**
 * The availability an outage of a request stands for.
 */
export function outageToServiceAvailability(failure: OutageFailure): ServiceAvailability {
  return failure.type === "network" ? { type: "noConnection" } : { type: "unreachable" };
}

/**
 * The availability a failed request stands for, or undefined for a failure that is a defect rather than an outage.
 */
export function failureToServiceAvailability(error: unknown): ServiceAvailability | undefined {
  const failure = classifyHttpFailure(error);
  return failure.type === "other" ? undefined : outageToServiceAvailability(failure);
}

/**
 * The availability a failed health poll stands for, or undefined for a defect. The service answers `/health` with 200 in
 * every state, so any 5xx comes from in front of it, such as a login gateway, and counts as unreachable.
 */
export function pollFailureToServiceAvailability(error: unknown): ServiceAvailability | undefined {
  const status = isAxiosError(error) ? error.response?.status : undefined;
  return failureToServiceAvailability(error) ?? (status !== undefined && status >= 500 ? { type: "unreachable" } : undefined);
}

/**
 * State of the health poll query, as `useQuery` and `QueryClient.getQueryState` report it.
 */
export interface HealthPollState {
  data: HealthResponse | undefined;
  error: unknown;
}

/**
 * The availability the latest poll answer stands for; a successful answer clears the error of the one before.
 * Undefined before the first answer and for a failure that is a defect.
 */
export function pollStateToServiceAvailability(state: HealthPollState): ServiceAvailability | undefined {
  if (state.error !== null && state.error !== undefined) {
    // An ended session hides the state of the service; the outage ends, so a recovery round remounts the auth
    // provider, which shows the sign-in-again screen.
    if (reactionToFailure(state.error, "query").type === "sessionExpired") {
      return { type: "available", state: "ready" };
    }
    return pollFailureToServiceAvailability(state.error);
  }
  return state.data === undefined ? undefined : toServiceAvailability(state.data);
}

/**
 * Listens to the window's online and offline events to update the device's connectivity status.
 */
export function useDeviceOnlineListener(mockOffline: boolean = false): void {
  const setDeviceOnline = useSetAtom(deviceOnlineAtom);

  // check for network connection
  useEffect(() => {
    if (!mockOffline) {
      const updateOnlineStatus = () => {
        setDeviceOnline(window.navigator.onLine);
      };

      // listen for changes
      window.addEventListener("online", updateOnlineStatus);
      window.addEventListener("offline", updateOnlineStatus);

      updateOnlineStatus(); // Initialize

      // remove event listeners when component is unmounted
      return () => {
        window.removeEventListener("online", updateOnlineStatus);
        window.removeEventListener("offline", updateOnlineStatus);
      };
    } else {
      setDeviceOnline(false);
      return undefined;
    }
  }, [mockOffline, setDeviceOnline]);
}

/**
 * Polls the service's health endpoint and keeps [serviceAvailabilityAtom] up to date.
 * A 3xx or 4xx answer is thrown as the query client throws it for every query; a same-origin 401 also ends the outage,
 * and under `unauthorizedEndsSession` it is not thrown.
 */
export function useServiceHealthPoll(
  healthCheckFn: () => Promise<HealthResponse>,
  refetchInterval: number = DefaultHealthPollIntervalMs,
  mockAvailability?: ServiceAvailability,
): void {
  const isDeviceOnline = useAtomValueRawSync(deviceOnlineAtom);
  const setServiceAvailability = useSetAtom(serviceAvailabilityAtom);
  const setLastPollAnswerAt = useSetAtom(lastPollAnswerAtAtom);
  const defaultThrowOnError = useQueryClient().getDefaultOptions().queries?.throwOnError;

  const { data, error, dataUpdatedAt, errorUpdatedAt } = useQueryPrimitive({
    queryKey: HealthCheckQueryKey,
    queryFn: healthCheckFn,
    refetchInterval,
    retry: false,
    // An outage, a 5xx included, sets the banner; every other failure follows the client's default, so a 401 ends the session where it does.
    throwOnError: (pollError, query) =>
      pollFailureToServiceAvailability(pollError) === undefined &&
      // The default is typed for an untyped query; it reads only the error and `query.state`.
      (typeof defaultThrowOnError === "function" ? defaultThrowOnError(pollError, query as never) : defaultThrowOnError !== false),
    // A reset by a successful mutation would drop the last answer, and with it the banner, until the next poll.
    meta: { preventBeingResetByMutations: true },
    enabled: isDeviceOnline && mockAvailability === undefined,
  });

  useEffect(() => {
    if (mockAvailability !== undefined) {
      setServiceAvailability(mockAvailability);
      return;
    }

    const availability = pollStateToServiceAvailability({ data, error });
    if (availability !== undefined) {
      setServiceAvailability(availability);
    }
    const answeredAt = Math.max(dataUpdatedAt, errorUpdatedAt);
    setLastPollAnswerAt(answeredAt === 0 ? undefined : answeredAt);
  }, [data, error, dataUpdatedAt, errorUpdatedAt, setServiceAvailability, setLastPollAnswerAt, mockAvailability]);
}

// useAtomValueRawSync, because a reader in the same component as useDeviceOnlineListener misses its mount-time write with useAtomValue since jotai 3.
export function useDeviceIsOnline(): boolean {
  return useAtomValueRawSync(deviceOnlineAtom);
}

/**
 * The availability of the last health poll; undefined until the first poll answers.
 */
export function useServiceAvailability(): ServiceAvailability | undefined {
  return useAtomValueRawSync(serviceAvailabilityAtom);
}

/**
 * Epoch milliseconds of the latest health poll answer; undefined until the first one.
 */
export function useLastPollAnswerAt(): number | undefined {
  return useAtomValueRawSync(lastPollAnswerAtAtom);
}

/**
 * The current recovery round; `ServiceUnavailablePage` compares it with the round it appeared in.
 */
export function useRecoveryRound(): number {
  return useAtomValueRawSync(recoveryRoundAtom);
}

export function useStartRecoveryRound(): () => void {
  const setRecoveryRound = useSetAtom(recoveryRoundAtom);
  return useCallback(() => setRecoveryRound((round) => round + 1), [setRecoveryRound]);
}
