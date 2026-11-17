/**
 * Imports
 */

import { PlatformHeader } from "@neckar-hub/typescript-utils/platform-headers";
import { AxiosError } from "axios";

/**
 * Response header carrying the OpenTelemetry server-span trace ID (32 hex
 * characters), set by `XTraceIdHeaderPlugin` and exposed to JavaScript through
 * `installProductCors()` on every Ktor service response. Axios lower-cases all
 * response header keys, so the shared constant is lower-cased for the lookup.
 */
const TraceIdHeader = PlatformHeader.TraceId.toLowerCase();

/**
 * Body field carrying the trace ID on every structured error payload. Stable
 * server/frontend contract — must match `ApplicationError.traceId` in the Kotlin
 * code.
 */
const TraceIdBodyField = "traceId";

/**
 * Extracts the OpenTelemetry trace ID for a caught error so the error page can
 * link it to its Tempo trace in Grafana.
 *
 * Source order:
 * 1. `NeckarIT-Trace-Id` response header — primary, present on every backend
 *    response that ran inside a span.
 * 2. `response.data.traceId` — the field every structured error body carries,
 *    so a stored or forwarded body keeps the trace ID.
 * 3. Optional app-supplied getter for a locally generated trace ID (errors that
 *    never reached the backend). Injected by the app, never pulled through a
 *    direct `@opentelemetry/api` import — this library stays free of the OTel
 *    web SDK.
 *
 * Returns `undefined` when no trace ID is available (e.g. a pure browser error
 * before the first backend call), so callers can omit the trace block entirely.
 */
export function extractTraceId(error: unknown, getCurrentTraceId?: () => string | undefined): string | undefined {
  if (error instanceof AxiosError) {
    const headerTraceId = error.response?.headers?.[TraceIdHeader];
    if (typeof headerTraceId === "string" && headerTraceId.length > 0) {
      return headerTraceId;
    }

    const body = error.response?.data as Record<string, unknown> | undefined;
    const bodyTraceId = body?.[TraceIdBodyField];
    if (typeof bodyTraceId === "string" && bodyTraceId.length > 0) {
      return bodyTraceId;
    }
  }

  return getCurrentTraceId?.();
}
