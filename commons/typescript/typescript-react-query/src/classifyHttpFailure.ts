/**
 * Imports
 */

import { HttpStatusCodes } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { isAxiosError } from "axios";

/**
 * What a failed request says about the service behind it.
 *
 * - `network`: no response; the request did not reach Traefik, or the connection broke before the answer (device offline, DNS, TLS,
 *   connection lost). There is no status.
 * - `badGateway`: 502, Traefik found no process behind the router.
 * - `serviceNotReady`: 503, the service runs and rejects domain routes because it is not `ready`; the request was not executed.
 * - `gatewayTimeout`: 504, the service accepted the request and did not answer in time; the outcome is unknown.
 * - `other`: every other failure, among them 3xx, 4xx, 500 and the remaining 5xx.
 */
export type HttpFailure =
  | { type: "network" }
  | { type: "badGateway" }
  | { type: "serviceNotReady" }
  | { type: "gatewayTimeout" }
  | { type: "other" };

/**
 * Messages of the `TypeError` a `fetch()` call rejects with on a network error, per browser.
 * The message tells it apart from a `TypeError` thrown by a programming error.
 * Safari 16 passes the system error text in the device language; only English devices match.
 */
const FetchNetworkErrorMessages = new Set([
  "Failed to fetch", // Chromium
  "NetworkError when attempting to fetch resource.", // Firefox
  "Load failed", // Safari 17+
  "The Internet connection appears to be offline.", // Safari 16, device offline
  "Could not connect to the server.", // Safari 16, server down or port closed
  "A server with the specified hostname could not be found.", // Safari 16, DNS failure
  "The network connection was lost.", // Safari 16, connection dropped
]);

function isFetchNetworkError(error: unknown): boolean {
  return error instanceof TypeError && FetchNetworkErrorMessages.has(error.message);
}

export function classifyHttpFailure(error: unknown): HttpFailure {
  if (isFetchNetworkError(error)) {
    return { type: "network" };
  }

  if (!isAxiosError(error)) {
    return { type: "other" };
  }

  const httpStatus = error.response?.status;
  switch (httpStatus) {
    case undefined:
      // A cancelled request has no response either, and is no network failure.
      return error.code === "ERR_CANCELED" ? { type: "other" } : { type: "network" };
    case HttpStatusCodes.BadGateway:
      return { type: "badGateway" };
    case HttpStatusCodes.ServiceUnavailable:
      return { type: "serviceNotReady" };
    case HttpStatusCodes.GatewayTimeout:
      return { type: "gatewayTimeout" };
    default:
      return { type: "other" };
  }
}

/**
 * Whether [error] says the service is out of reach or not ready, as opposed to a defect or a rejected request.
 */
export function isServiceUnavailable(error: unknown): boolean {
  return classifyHttpFailure(error).type !== "other";
}
