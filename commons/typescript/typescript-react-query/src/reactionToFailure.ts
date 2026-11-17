/**
 * Imports
 */

import { type ExpectedApplicationError } from "@neckar-hub/typescript-utils/openapi-types";
import { isExpectedApplicationError, isModuleLoadError } from "@neckar-hub/typescript-utils/type-guards";
import { isAxiosError, isCancel } from "axios";
import { classifyHttpFailure, type HttpFailure } from "./classifyHttpFailure";
import { isSameOriginUnauthorized } from "./isSameOriginUnauthorized";

/**
 * Prefix of the error codes `RestProtocolVersionProblemException.Cause` sends.
 */
const ProtocolVersionErrorCodePrefix = "PVE-";

/**
 * Error code of `it.neckar.open.authorization.MissingCapabilityException`.
 */
const MissingCapabilityErrorCode = "AUTHZ-1";

/**
 * Attribute that names the missing capability; matches `MissingCapabilityException.MissingCapabilityAttribute`.
 */
const MissingCapabilityAttribute = "missingCapability";

/**
 * The request a failure belongs to: a query without data on screen, a query with data on screen, or a mutation.
 */
export type RequestKind = "query" | "queryWithData" | "mutation";

/**
 * A failure that says the service is out of reach or not ready.
 */
export type OutageFailure = Exclude<HttpFailure, { type: "other" }>;

/**
 * What the application shows for a failed request. [reactionToFailure] is the only place that decides it.
 *
 * - `cancelled`: the caller cancelled a mutation; nothing to show.
 * - `keepDataWithBanner`: an outage of a query with data on screen; the data stays, the banner names the service state.
 * - `serviceUnavailablePage`: an outage of a query without data on screen; `ServiceUnavailablePage` recovers by itself under a health poll.
 * - `notExecuted`: a 502 or 503 of a mutation; the service did not run it, the form stays open.
 * - `unknownOutcome`: a 504 or network error of a mutation; the service may have run it, the form stays open.
 * - `sessionExpired`: a 401 of the page's own origin.
 * - `permissionDenied`: a 403 with `errorCode` `AUTHZ-1` and a string `attributes.missingCapability`.
 * - `versionMismatch`: an `errorCode` starting with `PVE-`, or a lazy chunk that does not load after a deploy.
 * - `errorPage`: every other failure.
 */
export type FailureReaction =
  | { type: "cancelled" }
  | { type: "keepDataWithBanner"; failure: OutageFailure }
  | { type: "serviceUnavailablePage"; failure: OutageFailure }
  | { type: "notExecuted"; failure: Extract<OutageFailure, { type: "badGateway" | "serviceNotReady" }> }
  | { type: "unknownOutcome" }
  | { type: "sessionExpired" }
  | { type: "permissionDenied"; missingCapability: string; payload: ExpectedApplicationError }
  | { type: "versionMismatch" }
  | { type: "errorPage" };

export type QueryFailureReaction = Exclude<
  FailureReaction,
  { type: "cancelled" | "keepDataWithBanner" | "notExecuted" | "unknownOutcome" }
>;

export type QueryWithDataFailureReaction = Exclude<
  FailureReaction,
  { type: "cancelled" | "serviceUnavailablePage" | "notExecuted" | "unknownOutcome" }
>;

export type MutationFailureReaction = Exclude<FailureReaction, { type: "keepDataWithBanner" | "serviceUnavailablePage" }>;

/**
 * The reaction to [error] of a request of [request] kind.
 */
export function reactionToFailure(error: unknown, request: "query"): QueryFailureReaction;
export function reactionToFailure(error: unknown, request: "queryWithData"): QueryWithDataFailureReaction;
export function reactionToFailure(error: unknown, request: "mutation"): MutationFailureReaction;
export function reactionToFailure(error: unknown, request: RequestKind): FailureReaction;
export function reactionToFailure(error: unknown, request: RequestKind): FailureReaction {
  if (request === "mutation" && isCancel(error)) {
    return { type: "cancelled" };
  }
  if (isModuleLoadError(error) || hasProtocolVersionErrorCode(error)) {
    return { type: "versionMismatch" };
  }
  if (isSameOriginUnauthorized(error)) {
    return { type: "sessionExpired" };
  }

  const failure = classifyHttpFailure(error);
  if (failure.type !== "other") {
    return outageReaction(failure, request);
  }

  const permissionDenied = missingCapabilityOf(error);
  if (permissionDenied !== undefined) {
    return { type: "permissionDenied", ...permissionDenied };
  }
  return { type: "errorPage" };
}

function outageReaction(failure: OutageFailure, request: RequestKind): FailureReaction {
  switch (request) {
    case "query":
      return { type: "serviceUnavailablePage", failure };
    case "queryWithData":
      return { type: "keepDataWithBanner", failure };
    case "mutation":
      switch (failure.type) {
        case "badGateway":
        case "serviceNotReady":
          return { type: "notExecuted", failure };
        case "network":
        case "gatewayTimeout":
          // The request may have reached the service before the connection or the gateway gave up.
          return { type: "unknownOutcome" };
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(failure)}`);
      }
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(request)}`);
  }
}

function hasProtocolVersionErrorCode(error: unknown): boolean {
  if (!isAxiosError(error) || error.response === undefined || error.response.status < 400) {
    return false;
  }
  const payload: unknown = error.response.data;
  return isExpectedApplicationError(payload) && payload.errorCode.startsWith(ProtocolVersionErrorCodePrefix);
}

function missingCapabilityOf(error: unknown): { missingCapability: string; payload: ExpectedApplicationError } | undefined {
  if (!isAxiosError(error) || error.response?.status !== 403) {
    return undefined;
  }
  const payload: unknown = error.response.data;
  if (!isExpectedApplicationError(payload) || payload.errorCode !== MissingCapabilityErrorCode) {
    return undefined;
  }
  const missingCapability = payload.attributes?.[MissingCapabilityAttribute];
  return typeof missingCapability === "string" ? { missingCapability, payload } : undefined;
}
