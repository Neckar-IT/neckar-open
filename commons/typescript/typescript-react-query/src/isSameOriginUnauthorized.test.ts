/**
 * Imports
 */

import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from "axios";
import { describe, expect, it } from "vitest";
import { isSameOriginUnauthorized } from "./isSameOriginUnauthorized";

function httpError(status: number, config: Partial<InternalAxiosRequestConfig>): AxiosError {
  const requestConfig: InternalAxiosRequestConfig = { headers: new AxiosHeaders(), ...config };
  return new AxiosError("failed", AxiosError.ERR_BAD_REQUEST, requestConfig, undefined, {
    status,
    statusText: "",
    data: {},
    headers: {},
    config: requestConfig,
  });
}

/**
 * Tests
 */

describe("isSameOriginUnauthorized", () => {
  it("accepts a 401 of a relative URL", () => {
    expect(isSameOriginUnauthorized(httpError(401, { url: "/api/books/42" }))).toBe(true);
  });

  it("accepts a 401 of an absolute URL on the page's origin", () => {
    expect(isSameOriginUnauthorized(httpError(401, { url: `${window.location.origin}/api/books/42` }))).toBe(true);
  });

  it("rejects a 401 of a foreign origin", () => {
    expect(isSameOriginUnauthorized(httpError(401, { url: "https://other.example/api/x" }))).toBe(false);
  });

  it("rejects a 401 whose baseURL points to a foreign origin", () => {
    expect(isSameOriginUnauthorized(httpError(401, { baseURL: "https://other.example", url: "/api/x" }))).toBe(false);
  });

  it("rejects a 403 of the page's origin", () => {
    expect(isSameOriginUnauthorized(httpError(403, { url: "/api/books/42" }))).toBe(false);
  });

  it("rejects an axios error without a response", () => {
    expect(isSameOriginUnauthorized(new AxiosError("network down"))).toBe(false);
  });

  it("rejects an error that is no axios error", () => {
    expect(isSameOriginUnauthorized(new Error("boom"))).toBe(false);
  });
});
