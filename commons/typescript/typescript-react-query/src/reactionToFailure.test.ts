/**
 * Imports
 */

import { ExpectedApplicationErrorWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import { AxiosError, AxiosHeaders, CanceledError, type InternalAxiosRequestConfig } from "axios";
import { describe, expect, it } from "vitest";
import { type FailureReaction, reactionToFailure, type RequestKind } from "./reactionToFailure";

/**
 * Fixtures
 */

const config: InternalAxiosRequestConfig = { headers: new AxiosHeaders(), url: "/api/patterns/books" };
const foreignConfig: InternalAxiosRequestConfig = { headers: new AxiosHeaders(), url: "https://auth.example.com/token" };

function errorWithStatus(status: number, data?: unknown, requestConfig: InternalAxiosRequestConfig = config): AxiosError {
  return new AxiosError(`Request failed with status code ${status}`, AxiosError.ERR_BAD_RESPONSE, requestConfig, undefined, {
    data,
    status,
    statusText: "",
    headers: {},
    config: requestConfig,
  });
}

function expectedError(errorCode: string, attributes?: Record<string, string>) {
  return { type: ExpectedApplicationErrorWithTypeType.ExpectedError, errorCode, fallbackMessage: errorCode, attributes };
}

const missingCapabilityPayload = expectedError("AUTHZ-1", { missingCapability: "patterns:finalize-order" });

const errors = {
  network: new AxiosError("Network Error", AxiosError.ERR_NETWORK, config),
  fetchNetwork: new TypeError("Failed to fetch"),
  badGateway: errorWithStatus(502),
  serviceNotReady: errorWithStatus(503),
  gatewayTimeout: errorWithStatus(504),
  sameOriginUnauthorized: errorWithStatus(401),
  foreignUnauthorized: errorWithStatus(401, undefined, foreignConfig),
  missingCapability: errorWithStatus(403, missingCapabilityPayload),
  otherForbidden: errorWithStatus(403, expectedError("AUTHZ-2")),
  protocolVersion: errorWithStatus(400, expectedError("PVE-100")),
  moduleLoad: new TypeError("Failed to fetch dynamically imported module: https://app.example.com/assets/page-1a2b.js"),
  notFound: errorWithStatus(404),
  conflict: errorWithStatus(409),
  serverError: errorWithStatus(500),
  programmingError: new TypeError("Cannot read properties of undefined (reading 'id')"),
  cancelled: new CanceledError(),
};

const network = { type: "network" } as const;
const badGateway = { type: "badGateway" } as const;
const serviceNotReady = { type: "serviceNotReady" } as const;
const gatewayTimeout = { type: "gatewayTimeout" } as const;
const permissionDenied: FailureReaction = {
  type: "permissionDenied",
  missingCapability: "patterns:finalize-order",
  payload: missingCapabilityPayload,
};

/**
 * Tests: the table `docs/react/http-status-handling.html` shows
 */

describe("reactionToFailure", () => {
  it.each<[keyof typeof errors, FailureReaction, FailureReaction, FailureReaction]>([
    // error, query, queryWithData, mutation
    [
      "network",
      { type: "serviceUnavailablePage", failure: network },
      { type: "keepDataWithBanner", failure: network },
      { type: "unknownOutcome" },
    ],
    [
      "fetchNetwork",
      { type: "serviceUnavailablePage", failure: network },
      { type: "keepDataWithBanner", failure: network },
      { type: "unknownOutcome" },
    ],
    [
      "badGateway",
      { type: "serviceUnavailablePage", failure: badGateway },
      { type: "keepDataWithBanner", failure: badGateway },
      { type: "notExecuted", failure: badGateway },
    ],
    [
      "serviceNotReady",
      { type: "serviceUnavailablePage", failure: serviceNotReady },
      { type: "keepDataWithBanner", failure: serviceNotReady },
      { type: "notExecuted", failure: serviceNotReady },
    ],
    [
      "gatewayTimeout",
      { type: "serviceUnavailablePage", failure: gatewayTimeout },
      { type: "keepDataWithBanner", failure: gatewayTimeout },
      { type: "unknownOutcome" },
    ],
    ["sameOriginUnauthorized", { type: "sessionExpired" }, { type: "sessionExpired" }, { type: "sessionExpired" }],
    ["foreignUnauthorized", { type: "errorPage" }, { type: "errorPage" }, { type: "errorPage" }],
    ["missingCapability", permissionDenied, permissionDenied, permissionDenied],
    ["otherForbidden", { type: "errorPage" }, { type: "errorPage" }, { type: "errorPage" }],
    ["protocolVersion", { type: "versionMismatch" }, { type: "versionMismatch" }, { type: "versionMismatch" }],
    ["moduleLoad", { type: "versionMismatch" }, { type: "versionMismatch" }, { type: "versionMismatch" }],
    ["notFound", { type: "errorPage" }, { type: "errorPage" }, { type: "errorPage" }],
    ["conflict", { type: "errorPage" }, { type: "errorPage" }, { type: "errorPage" }],
    ["serverError", { type: "errorPage" }, { type: "errorPage" }, { type: "errorPage" }],
    ["programmingError", { type: "errorPage" }, { type: "errorPage" }, { type: "errorPage" }],
    ["cancelled", { type: "errorPage" }, { type: "errorPage" }, { type: "cancelled" }],
  ])("reacts to %s", (name, query, queryWithData, mutation) => {
    const error = errors[name];
    const reactions = (["query", "queryWithData", "mutation"] as RequestKind[]).map((request) => reactionToFailure(error, request));

    expect(reactions).toEqual([query, queryWithData, mutation]);
  });

  it("takes a 403 with AUTHZ-1 but without the capability attribute for an error page", () => {
    expect(reactionToFailure(errorWithStatus(403, expectedError("AUTHZ-1")), "query")).toEqual({ type: "errorPage" });
  });

  it("takes a PVE code on a successful status for no version mismatch", () => {
    expect(reactionToFailure(errorWithStatus(200, expectedError("PVE-100")), "query")).toEqual({ type: "errorPage" });
  });
});
