/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { makeAxiosError } from "../../axiosErrorFixture";
import type { ObservabilityConfig } from "../../ObservabilityContext";
import type { VersionInfoData } from "../../VersionInfoContext";
import { buildErrorInfoText } from "./errorInfoText";

/**
 * Helpers
 */

const TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
const OCCURRED_AT = "2026-09-11T09:30:00.000Z";
const SERVICE_HOST = "neckar-hub-01.neckar.it";
const versionInfo: VersionInfoData = {
  imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
  imageDigestShort: "ae4608c7c590",
};

/**
 * Tests
 */

describe("buildErrorInfoText", () => {
  it("includes request details, trace ID and Grafana URL for an axios error", () => {
    const observability: ObservabilityConfig = { traceLinkTemplate: "https://grafana.example/explore?trace=__TRACE_ID__" };
    const text = buildErrorInfoText({
      errorCaughtBy: "global-error-boundary",
      error: makeAxiosError({
        status: 500,
        statusText: "Internal Server Error",
        method: "post",
        url: "/api/patterns/books",
        responseData: { message: "boom" },
        traceId: TRACE_ID,
      }),
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability,
    });

    expect(text).toContain("Request:");
    expect(text).toContain("POST /api/patterns/books");
    expect(text).toContain("Status: 500 Internal Server Error");
    expect(text).toContain('"message": "boom"');
    expect(text).toContain(`Trace ID: ${TRACE_ID}`);
    expect(text).toContain(`Grafana: https://grafana.example/explore?trace=${TRACE_ID}`);
    expect(text).toContain("Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e");
  });

  it("omits the Grafana URL when no template is configured but keeps the trace ID", () => {
    const text = buildErrorInfoText({
      errorCaughtBy: "global-error-boundary",
      error: makeAxiosError({ status: 500, traceId: TRACE_ID }),
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: null,
    });

    expect(text).toContain(`Trace ID: ${TRACE_ID}`);
    expect(text).not.toContain("Grafana:");
  });

  it("names the exception type of a server error without a message", () => {
    const text = buildErrorInfoText({
      errorCaughtBy: "global-error-boundary",
      exceptionType: "java.lang.IllegalStateException",
      error: makeAxiosError({ status: 500, traceId: TRACE_ID }),
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: null,
    });

    expect(text).toContain("Exception Type: java.lang.IllegalStateException");
  });

  it("omits request and trace sections for a plain error", () => {
    const text = buildErrorInfoText({
      errorMessage: "plain failure",
      errorCaughtBy: "window-error-listener",
      error: new Error("plain failure"),
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: null,
    });

    expect(text).toContain("Error Message: plain failure");
    expect(text).not.toContain("Request:");
    expect(text).not.toContain("Trace ID:");
  });

  it("puts time, trace ID and version ahead of response body and stack trace", () => {
    const text = buildErrorInfoText({
      errorCaughtBy: "global-error-boundary",
      error: makeAxiosError({ status: 500, traceId: TRACE_ID, responseData: { message: "boom" } }),
      stackTrace: "at frame0",
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: null,
    });

    const firstLongSection = text.indexOf("Response Body:");
    expect(firstLongSection).toBeLessThan(text.indexOf("Stack Trace:"));
    for (const shortLine of [
      `Occurred At: ${OCCURRED_AT}`,
      `Service Host: ${SERVICE_HOST}`,
      `Trace ID: ${TRACE_ID}`,
      "Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
    ]) {
      expect(text.indexOf(shortLine)).toBeGreaterThan(-1);
      expect(text.indexOf(shortLine)).toBeLessThan(firstLongSection);
    }
  });

  it("states a service host the deployment did not inject as missing", () => {
    const text = buildErrorInfoText({
      errorCaughtBy: "window-error-listener",
      occurredAt: OCCURRED_AT,
      serviceHost: undefined,
      versionInfo,
      observability: null,
    });

    expect(text).toContain("Service Host: (not injected)");
  });

  it("keeps a multi-line message out of the short sections and writes it in full after them", () => {
    const errorMessage = `404 Not Found: ${JSON.stringify({ detail: "x".repeat(1000) }, null, 2)}`;

    const text = buildErrorInfoText({
      errorMessage,
      errorCaughtBy: "router-error-element",
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: null,
    });

    expect(text.split("\n")[0]).toBe("Error Message: 404 Not Found: { …");
    expect(text.indexOf("Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e")).toBeLessThan(
      text.indexOf("Full Error Message:"),
    );
    expect(text).toContain(errorMessage);
  });

  function textFor(errorMessage: string): string {
    return buildErrorInfoText({
      errorMessage,
      errorCaughtBy: "window-error-listener",
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: null,
    });
  }

  it("keeps a message of 200 bytes whole in the header", () => {
    const text = textFor("x".repeat(200));

    expect(text.split("\n")[0]).toBe(`Error Message: ${"x".repeat(200)}`);
    expect(text).not.toContain("Full Error Message:");
  });

  it("cuts the header by UTF-8 bytes and writes the full message after the short sections", () => {
    expect(textFor("x".repeat(201)).split("\n")[0]).toBe(`Error Message: ${"x".repeat(200)} …`);

    // Three bytes per character: 66 of them fit into 200 bytes.
    const cjk = textFor("错".repeat(200));
    expect(cjk.split("\n")[0]).toBe(`Error Message: ${"错".repeat(66)} …`);
    expect(cjk.indexOf("Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e")).toBeLessThan(
      cjk.indexOf("Full Error Message:"),
    );
  });

  it("neither cuts nor repeats a message that only ends in a line break", () => {
    expect(textFor("boom\n").split("\n")[0]).toBe("Error Message: boom");
    expect(textFor("boom\n")).not.toContain("Full Error Message:");
    expect(textFor("line1\r\nline2").split("\n")[0]).toBe("Error Message: line1 …");
  });
});
