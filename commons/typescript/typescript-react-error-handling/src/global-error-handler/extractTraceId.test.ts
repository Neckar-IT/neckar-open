/**
 * Imports
 */

import { AxiosError, type AxiosResponse } from "axios";
import { describe, expect, it } from "vitest";
import { extractTraceId } from "./extractTraceId";

/**
 * Helpers
 */

const TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

function axiosErrorWith(response: Partial<AxiosResponse> | undefined): AxiosError {
  const error = new AxiosError("boom");
  if (response !== undefined) {
    error.response = response as AxiosResponse;
  }
  return error;
}

/**
 * Tests
 */

describe("extractTraceId", () => {
  it("reads the trace ID from the NeckarIT-Trace-Id response header", () => {
    const error = axiosErrorWith({ headers: { "neckarit-trace-id": TRACE_ID }, data: undefined });
    expect(extractTraceId(error)).toBe(TRACE_ID);
  });

  it("reads the trace ID from the response body field when no header is present", () => {
    const error = axiosErrorWith({ headers: {}, data: { traceId: TRACE_ID } });
    expect(extractTraceId(error)).toBe(TRACE_ID);
  });

  it("prefers the header over the body field", () => {
    const error = axiosErrorWith({ headers: { "neckarit-trace-id": TRACE_ID }, data: { traceId: "body-trace-id" } });
    expect(extractTraceId(error)).toBe(TRACE_ID);
  });

  it("returns undefined when neither header nor body carries a trace ID", () => {
    const error = axiosErrorWith({ headers: {}, data: {} });
    expect(extractTraceId(error)).toBeUndefined();
  });

  it("returns undefined for an axios error without a response", () => {
    expect(extractTraceId(axiosErrorWith(undefined))).toBeUndefined();
  });

  it("falls back to the app-supplied getter when the error carries no trace ID", () => {
    const error = axiosErrorWith({ headers: {}, data: {} });
    expect(extractTraceId(error, () => "local-trace-id")).toBe("local-trace-id");
  });

  it("uses the app-supplied getter for a non-axios error", () => {
    expect(extractTraceId(new Error("plain"), () => "local-trace-id")).toBe("local-trace-id");
  });

  it("returns undefined for a non-axios error without a getter", () => {
    expect(extractTraceId(new Error("plain"))).toBeUndefined();
  });

  it("ignores an empty header value and falls through to the body", () => {
    const error = axiosErrorWith({ headers: { "neckarit-trace-id": "" }, data: { traceId: TRACE_ID } });
    expect(extractTraceId(error)).toBe(TRACE_ID);
  });
});
