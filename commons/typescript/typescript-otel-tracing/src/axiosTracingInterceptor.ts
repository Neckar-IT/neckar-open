import { PlatformHeader } from "@neckar-hub/typescript-utils/platform-headers";
import { type Span, SpanKind, SpanStatusCode, context, propagation, trace } from "@opentelemetry/api";
import { isCancel, type AxiosInstance } from "axios";
import { readSessionId } from "./sessionId";

const TRACER_NAME = "axios-http-client";

/**
 * Register OpenTelemetry tracing interceptors on an Axios instance.
 *
 * Request interceptor: Creates a span for each HTTP request and injects
 * W3C Trace Context headers (traceparent) for end-to-end propagation.
 *
 * Response interceptor: Ends the span with appropriate status based on
 * the HTTP response status code.
 */
export function registerAxiosTracingInterceptors(axiosInstance: AxiosInstance): void {
  // Request interceptor: start span and inject trace context
  axiosInstance.interceptors.request.use((config) => {
    const tracer = trace.getTracer(TRACER_NAME);
    const method = (config.method ?? "GET").toUpperCase();
    const url = config.url ?? "unknown";

    const span = tracer.startSpan(`HTTP ${method} ${url}`, {
      kind: SpanKind.CLIENT,
      attributes: {
        "http.request.method": method,
        "url.full": url,
        "page.route": window.location.pathname,
      },
    });

    // Store span in config for the response interceptor to access
    (config as unknown as Record<string, unknown>)._otelSpan = span;

    // Inject trace context (traceparent header) into request headers
    const traceHeaders: Record<string, string> = {};
    const activeContext = trace.setSpan(context.active(), span);

    propagation.inject(activeContext, traceHeaders, {
      set(carrier, key, value) {
        carrier[key] = value;
      },
    });

    for (const [key, value] of Object.entries(traceHeaders)) {
      config.headers.set(key, value);
    }

    // Propagate session ID so the backend can correlate all requests from this browser tab
    const sessionId = readSessionId();
    if (sessionId != null) {
      config.headers.set(PlatformHeader.SessionId, sessionId);
    }

    return config;
  });

  // Response interceptor: end span with status
  axiosInstance.interceptors.response.use(
    (response) => {
      const span = extractSpan(response.config);
      if (span != null) {
        span.setAttribute("http.response.status_code", response.status);
        span.setStatus({ code: SpanStatusCode.OK });
        span.end();
      }
      return response;
    },
    (error: unknown) => {
      const axiosError = error as { config?: unknown; response?: { status?: number }; message?: string } | undefined;
      const span = extractSpan(axiosError?.config);
      if (span != null) {
        if (isCancel(error)) {
          span.setAttribute("http.request.canceled", true);
          span.setStatus({ code: SpanStatusCode.UNSET });
        } else {
          const statusCode = axiosError?.response?.status;
          if (statusCode != null) {
            span.setAttribute("http.response.status_code", statusCode);
          }
          const message = axiosError?.message ?? "Request failed";
          span.setStatus({ code: SpanStatusCode.ERROR, message });
          span.recordException(error instanceof Error ? error : new Error(String(error)));
        }
        span.end();
      }
      throw error;
    },
  );
}

function extractSpan(config: unknown): Span | null {
  const record = config as Record<string, unknown> | undefined;
  const span = record?._otelSpan;
  if (span != null && typeof span === "object" && "end" in span) {
    return span as Span;
  }
  return null;
}
