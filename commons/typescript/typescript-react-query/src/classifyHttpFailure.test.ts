/**
 * Imports
 */

import { AxiosError, AxiosHeaders, CanceledError, type InternalAxiosRequestConfig } from "axios";
import { describe, expect, it } from "vitest";
import { classifyHttpFailure, type HttpFailure, isServiceUnavailable } from "./classifyHttpFailure";

/**
 * Fixtures
 */

const config: InternalAxiosRequestConfig = { headers: new AxiosHeaders(), url: "/api/patterns/books" };

function errorWithStatus(status: number): AxiosError {
  return new AxiosError(`Request failed with status code ${status}`, AxiosError.ERR_BAD_RESPONSE, config, undefined, {
    data: undefined,
    status,
    statusText: "",
    headers: {},
    config,
  });
}

/**
 * Tests
 */

describe("classifyHttpFailure", () => {
  it.each<[string, unknown, HttpFailure]>([
    ["301", errorWithStatus(301), { type: "other" }],
    ["400", errorWithStatus(400), { type: "other" }],
    ["401", errorWithStatus(401), { type: "other" }],
    ["403", errorWithStatus(403), { type: "other" }],
    ["404", errorWithStatus(404), { type: "other" }],
    ["409", errorWithStatus(409), { type: "other" }],
    ["500", errorWithStatus(500), { type: "other" }],
    ["501", errorWithStatus(501), { type: "other" }],
    ["502", errorWithStatus(502), { type: "badGateway" }],
    ["503", errorWithStatus(503), { type: "serviceNotReady" }],
    ["504", errorWithStatus(504), { type: "gatewayTimeout" }],
    ["505", errorWithStatus(505), { type: "other" }],
    ["an AxiosError without response", new AxiosError("Network Error", AxiosError.ERR_NETWORK, config), { type: "network" }],
    ["a fetch() TypeError", new TypeError("Failed to fetch"), { type: "network" }],
    ["a cancelled request", new CanceledError(undefined, undefined, config), { type: "other" }],
    ["an Error with the fetch message", new Error("Failed to fetch"), { type: "other" }],
    ["a programming TypeError", new TypeError("Cannot read properties of undefined (reading 'id')"), { type: "other" }],
    ["a string", "failed", { type: "other" }],
  ])("classifies %s", (_name, error, expected) => {
    expect(classifyHttpFailure(error)).toEqual(expected);
  });
});

describe("isServiceUnavailable", () => {
  it("holds for 502, 503, 504 and a network error", () => {
    expect(
      [errorWithStatus(502), errorWithStatus(503), errorWithStatus(504), new TypeError("Load failed")].map(isServiceUnavailable),
    ).toEqual([true, true, true, true]);
  });

  it("does not hold for 404 and 500", () => {
    expect([errorWithStatus(404), errorWithStatus(500)].map(isServiceUnavailable)).toEqual([false, false]);
  });
});

describe("classifyHttpFailure of a fetch() rejection", () => {
  it.each([
    ["Chromium", "Failed to fetch"],
    ["Firefox", "NetworkError when attempting to fetch resource."],
    ["Safari 17+", "Load failed"],
    ["Safari 16", "The Internet connection appears to be offline."],
    ["Safari 16", "Could not connect to the server."],
    ["Safari 16", "A server with the specified hostname could not be found."],
    ["Safari 16", "The network connection was lost."],
  ])("classifies the network error of %s: %s", (_browser, message) => {
    expect(classifyHttpFailure(new TypeError(message))).toEqual({ type: "network" });
  });

  it("does not take an abort for a network error", () => {
    expect(classifyHttpFailure(new DOMException("The operation was aborted.", "AbortError"))).toEqual({ type: "other" });
  });
});
