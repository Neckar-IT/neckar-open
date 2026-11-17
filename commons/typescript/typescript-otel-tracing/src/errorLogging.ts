import { logs, SeverityNumber } from "@opentelemetry/api";
import { reportScriptError } from "./clientErrorReporter";
import { httpResponseStatus, isExpectedError } from "./expectedErrors";

const LOGGER_NAME = "frontend-error-logger";

/**
 * Source that captured the error — used as an attribute on the log record
 * to distinguish where the error was caught.
 */
export type ErrorSource = "window-error" | "unhandled-rejection" | "error-boundary" | "mutation" | "manual";

/**
 * Capture an error and send it as an OTEL log record to the Collector.
 *
 * Skipped cases:
 *  - 4xx HTTP errors (already recorded as failed spans by the Axios tracing
 *    interceptor; the 4xx is a client-side expectation, not a frontend bug).
 *  - Canceled requests (AbortController) — not errors.
 *
 * 5xx HTTP errors are *not* skipped: the Axios span carries only
 * `http.response.status_code` plus Axios-internal frames, while the
 * React-Error-Boundary path carries the actual component stack that pinpoints
 * which page mounted the throwing query. Emit both so the log links to the
 * span via trace_id/span_id and adds the React context the span lacks.
 */
export function captureError(error: unknown, source: ErrorSource): void {
  if (isExpectedError(error)) {
    return;
  }
  const httpStatus = httpResponseStatus(error);

  // Before the login `/otlp/` rejects the export below. The reporter catches the window events with its own listeners.
  if (source !== "window-error" && source !== "unhandled-rejection") {
    reportScriptError(error);
  }

  const logger = logs.getLogger(LOGGER_NAME);
  const { message, stack } = extractErrorDetails(error);

  const attributes: Record<string, string | number> = {
    "error.type": errorTypeName(error),
    "error.message": message,
    "error.stack": stack,
    "error.source": source,
    "browser.url": window.location.href,
    "browser.user_agent": globalThis.navigator.userAgent,
  };
  if (httpStatus != null) {
    attributes["http.response.status_code"] = httpStatus;
  }

  logger.emit({
    severityNumber: SeverityNumber.ERROR,
    severityText: "ERROR",
    body: message,
    attributes,
  });
}

function extractErrorDetails(error: unknown): { message: string; stack: string } {
  if (error instanceof Error) {
    return {
      message: error.message,
      stack: error.stack ?? "",
    };
  }

  return {
    message: String(error),
    stack: "",
  };
}

function errorTypeName(error: unknown): string {
  if (error instanceof Error) {
    return error.constructor.name;
  }
  return typeof error;
}
