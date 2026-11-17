/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { makeAxiosError } from "../../axiosErrorFixture";
import { buildTraceLinkUrl, TraceIdPlaceholder } from "../../ObservabilityContext";
import type { VersionInfoData } from "../../VersionInfoContext";
import { buildErrorInfoText } from "./errorInfoText";
import { buildSupportMailtoUrl, MaxMailtoUrlLength } from "./supportMailtoUrl";

/**
 * Helpers
 */

const TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
const OCCURRED_AT = "2026-09-11T09:30:00.000Z";
const SERVICE_HOST = "neckar-hub-01.neckar.it";
const TRUNCATION_HINT = "[Truncated]";
const versionInfo: VersionInfoData = {
  imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
  imageDigestShort: "ae4608c7c590",
};

/**
 * A browser stack trace of 40 frames; as the message of a 400 body it pushes a support mail past its limit.
 */
const longStackTrace = Array.from({ length: 40 }, (_, index) => `    at frame${index} (app.js:${index}:1)`).join("\n");

function errorInfo(options: { errorMessage?: string; stackTrace?: string; responseData?: unknown } = {}): string {
  return buildErrorInfoText({
    errorMessage: options.errorMessage,
    errorCaughtBy: "global-error-boundary",
    error: makeAxiosError({ status: 400, traceId: TRACE_ID, responseData: options.responseData }),
    stackTrace: options.stackTrace,
    occurredAt: OCCURRED_AT,
    serviceHost: SERVICE_HOST,
    versionInfo,
    observability: null,
  });
}

function mailtoUrl(contactEmail: string, info: string): string {
  return buildSupportMailtoUrl({
    contactEmail,
    subject: "Error report",
    bodyIntro: "Hello,",
    errorInfo: info,
    truncationHint: TRUNCATION_HINT,
  });
}

function decodedBody(url: string): string {
  return decodeURIComponent(url.split("&body=")[1] ?? "");
}

/**
 * Tests
 */

describe("buildSupportMailtoUrl", () => {
  it("addresses the configured contact and carries subject, time and trace ID", () => {
    const url = mailtoUrl("support@example.com", errorInfo());

    expect(url.startsWith(`mailto:support@example.com?subject=${encodeURIComponent("Error report")}&body=`)).toBe(true);
    expect(decodedBody(url)).toContain(`Trace ID: ${TRACE_ID}`);
    expect(decodedBody(url)).toContain(`Occurred At: ${OCCURRED_AT}`);
    expect(decodedBody(url)).not.toContain(TRUNCATION_HINT);
  });

  it("truncates only once the complete URL exceeds the limit", () => {
    const frameLength = mailtoUrl("support@example.com", "").length;

    const atLimit = mailtoUrl("support@example.com", "x".repeat(MaxMailtoUrlLength - frameLength));
    const overLimit = mailtoUrl("support@example.com", "x".repeat(MaxMailtoUrlLength - frameLength + 1));

    expect(atLimit.length).toBe(MaxMailtoUrlLength);
    expect(decodedBody(atLimit)).not.toContain(TRUNCATION_HINT);
    expect(decodedBody(overLimit).endsWith(TRUNCATION_HINT)).toBe(true);
  });

  it("fills the URL limit with as much error information as fits and closes it with the hint", () => {
    const stackTrace = Array.from({ length: 500 }, (_, index) => `    at frame${index} (app.js:${index}:1)`).join("\n");

    const url = mailtoUrl("support@example.com", errorInfo({ stackTrace }));

    // One more ASCII character or line break adds at most three encoded characters.
    expect(url.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
    expect(url.length).toBeGreaterThan(MaxMailtoUrlLength - 3);
    expect(decodedBody(url).endsWith(TRUNCATION_HINT)).toBe(true);
    expect(decodedBody(url)).toContain("at frame0 ");
    expect(decodedBody(url)).not.toContain("at frame499 ");
  });

  it("keeps time, trace ID and version when the response body and the stack trace fill the limit", () => {
    const url = mailtoUrl(
      "support@example.com",
      errorInfo({
        errorMessage: "Unexpected Application Error",
        stackTrace: longStackTrace,
        responseData: {
          type: "UnexpectedError",
          fallbackMessage: longStackTrace,
          exceptionType: "java.lang.IllegalStateException",
        },
      }),
    );

    expect(url.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
    expect(decodedBody(url)).toContain(`Occurred At: ${OCCURRED_AT}`);
    expect(decodedBody(url)).toContain(`Service Host: ${SERVICE_HOST}`);
    expect(decodedBody(url)).toContain(`Trace ID: ${TRACE_ID}`);
    expect(decodedBody(url)).toContain("Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e");
    expect(decodedBody(url).endsWith(TRUNCATION_HINT)).toBe(true);
  });

  it("keeps version, trace ID and a Grafana link of production length", () => {
    const panes = encodeURIComponent(
      JSON.stringify({
        trace: {
          datasource: "tempo",
          queries: [{ refId: "A", datasource: { type: "tempo", uid: "tempo" }, queryType: "traceql", query: TraceIdPlaceholder }],
          range: { from: "now-1h", to: "now" },
        },
      }),
    );
    const traceLinkTemplate = `https://monitoring.neckar.it/explore?schemaVersion=1&orgId=1&panes=${panes}`;
    const info = buildErrorInfoText({
      errorMessage: "Unexpected Application Error",
      errorCaughtBy: "global-error-boundary",
      error: makeAxiosError({ status: 400, traceId: TRACE_ID, responseData: { fallbackMessage: longStackTrace } }),
      stackTrace: longStackTrace,
      occurredAt: OCCURRED_AT,
      serviceHost: SERVICE_HOST,
      versionInfo,
      observability: { traceLinkTemplate },
    });

    const url = mailtoUrl("support@example.com", info);

    expect(url.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
    expect(decodedBody(url)).toContain("Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e");
    expect(decodedBody(url)).toContain(`Trace ID: ${TRACE_ID}`);
    expect(decodedBody(url)).toContain(`Grafana: ${buildTraceLinkUrl(traceLinkTemplate, TRACE_ID)}`);
    expect(decodedBody(url).endsWith(TRUNCATION_HINT)).toBe(true);
  });

  it("truncates error information of emojis at every position inside an encoded emoji", () => {
    // An emoji encodes to twelve characters, so twelve offsets move the limit through all of them.
    for (let offset = 0; offset < 12; offset++) {
      const url = mailtoUrl("support@example.com", `${"x".repeat(offset)}${"😀".repeat(1500)}`);

      expect(url.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
      expect(url.length).toBeGreaterThan(MaxMailtoUrlLength - 12);
      expect(decodedBody(url).endsWith(TRUNCATION_HINT)).toBe(true);
    }
  });

  it("holds the limit for an error message of umlauts on a single line", () => {
    const url = mailtoUrl("support@example.com", errorInfo({ errorMessage: "Ä".repeat(3000) }));

    expect(url.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
    expect(decodedBody(url).endsWith(TRUNCATION_HINT)).toBe(true);
  });

  it("replaces a lone surrogate instead of failing to encode it", () => {
    const url = mailtoUrl("support@example.com", "cut emoji \uD83D");

    expect(decodedBody(url)).toContain("cut emoji \uFFFD");
  });

  it("keeps time, version and trace ID behind a first line of CJK text", () => {
    const url = mailtoUrl("support@example.com", errorInfo({ errorMessage: "错".repeat(200), stackTrace: longStackTrace }));

    expect(url.length).toBeLessThanOrEqual(MaxMailtoUrlLength);
    expect(decodedBody(url)).toContain(`Occurred At: ${OCCURRED_AT}`);
    expect(decodedBody(url)).toContain("Image Digest: sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e");
    expect(decodedBody(url)).toContain(`Trace ID: ${TRACE_ID}`);
  });

  it("replaces a lone low surrogate as well", () => {
    const url = mailtoUrl("support@example.com", `a${String.fromCharCode(0xde00)}b`);

    expect(decodedBody(url)).toContain(`a${String.fromCharCode(0xfffd)}b`);
  });

  it("refuses a frame that leaves no room for the error information", () => {
    const build = () =>
      buildSupportMailtoUrl({
        contactEmail: "support@example.com",
        subject: "x".repeat(MaxMailtoUrlLength),
        bodyIntro: "Hello,",
        errorInfo: errorInfo(),
        truncationHint: TRUNCATION_HINT,
      });

    expect(build).toThrow(/alone exceed/);
  });
});
