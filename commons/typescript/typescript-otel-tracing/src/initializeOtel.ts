import { type TextMapSetter, type Tracer, context, logs, propagation, trace } from "@opentelemetry/api";
import { W3CTraceContextPropagator } from "@opentelemetry/core";
import { OTLPLogExporter } from "@opentelemetry/exporter-logs-otlp-http";
import { OTLPTraceExporter } from "@opentelemetry/exporter-trace-otlp-http";
import { resourceFromAttributes } from "@opentelemetry/resources";
import { BatchLogRecordProcessor, LoggerProvider } from "@opentelemetry/sdk-logs";
import { BatchSpanProcessor, type SpanProcessor, StackContextManager, TracerProvider } from "@opentelemetry/sdk-trace";
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from "@opentelemetry/semantic-conventions";
import { IdentitySpanProcessor } from "./IdentitySpanProcessor";
import { getOrCreateSessionId } from "./sessionId";

/**
 * Build same-origin OTLP endpoint URLs. The Traefik router of the frontend forwards /otlp/* to otlp-gate of the host,
 * so no cross-origin requests and no CORS configuration needed.
 *
 * Constructed lazily inside initializeOtel() to avoid accessing `window` at module load
 * time, which would crash in non-browser environments (Node.js tests, SSR).
 */
function buildOtlpTracesUrl(): string {
  return `${window.location.origin}/otlp/v1/traces`;
}

function buildOtlpLogsUrl(): string {
  return `${window.location.origin}/otlp/v1/logs`;
}

export interface OtelConfig {
  /** Service name — must match the docker-compose service/container name (e.g., "patterns", "turboangebot") */
  serviceName: string;
  /** Optional service version, defaults to "unknown" */
  serviceVersion?: string;
  /**
   * Headers of every export, read anew each time: `keycloakOtlpHeaders` for a keycloak-js login, whose access token
   * otlp-gate checks. Omitted behind a Traefik login, where the session cookie travels on its own.
   */
  exportHeaders?: () => Promise<Record<string, string>>;
}

/**
 * Budget of one export, header lookup included: the exporter starts its abort timer before it awaits `exportHeaders`,
 * and a keycloak-js login lets that lookup wait up to 30 seconds.
 */
const ExportTimeoutMillis = 45_000;

let tracerProvider: TracerProvider | null = null;
let loggerProvider: LoggerProvider | null = null;
let identityProcessor: IdentitySpanProcessor | null = null;

/**
 * Determine the device size category based on viewport width.
 *
 * Uses `window.innerWidth` thresholds:
 * - <= 768px → "mobile"
 * - <= 1024px → "tablet"
 * - > 1024px → "desktop"
 */
function detectDeviceSize(): string {
  const width = window.innerWidth;
  if (width <= 768) return "mobile";
  if (width <= 1024) return "tablet";
  return "desktop";
}

/**
 * Initialize OpenTelemetry tracing and logging for a browser application.
 *
 * Sets up a TracerProvider with OTLP-HTTP export via the same-origin
 * /otlp/ path (forwarded to otlp-gate by the frontend's Traefik router) and W3C Trace
 * Context propagation. Also sets up a LoggerProvider for OTLP log export
 * to Loki via the otel-agent.
 *
 * Call this once in main.tsx before rendering the app.
 */
export function initializeOtel(config: OtelConfig): void {
  if (tracerProvider != null) {
    console.warn("[OTEL] Already initialized, skipping");
    return;
  }

  const resource = resourceFromAttributes({
    [ATTR_SERVICE_NAME]: config.serviceName,
    [ATTR_SERVICE_VERSION]: config.serviceVersion ?? "unknown",
    // Browser host (e.g. patterns.localdev, patterns2.localdev, patterns.neckar.it)
    // — matches the backend's neckar.service_host emitted by
    // configureOtelServiceHostOverride, so the Grafana Patterns dashboard can filter
    // frontend and backend telemetry under the same Service Host dropdown value.
    // Identical mechanism for local dev and production (where the value comes from
    // the SERVICE_HOST env var in docker-compose.yml).
    "neckar.service_host": window.location.host,
    "session.id": getOrCreateSessionId(),
    "device.size": detectDeviceSize(),
  });

  // Traces
  const traceExporter = new OTLPTraceExporter({
    url: buildOtlpTracesUrl(),
    headers: config.exportHeaders,
    timeoutMillis: ExportTimeoutMillis,
  });

  const batchProcessor: SpanProcessor = new BatchSpanProcessor({
    exporter: traceExporter,
    maxQueueSize: 100,
    maxExportBatchSize: 30,
    scheduledDelayMillis: 5000,
    exportTimeoutMillis: ExportTimeoutMillis,
  });

  identityProcessor = new IdentitySpanProcessor();

  tracerProvider = new TracerProvider({
    resource,
    spanProcessors: [identityProcessor, batchProcessor],
  });

  context.setGlobalContextManager(new StackContextManager().enable());
  propagation.setGlobalPropagator(new W3CTraceContextPropagator());
  trace.setGlobalTracerProvider(tracerProvider);

  // Logs
  const logExporter = new OTLPLogExporter({
    url: buildOtlpLogsUrl(),
    headers: config.exportHeaders,
    timeoutMillis: ExportTimeoutMillis,
  });

  const logProcessor = new BatchLogRecordProcessor({
    exporter: logExporter,
    maxQueueSize: 100,
    maxExportBatchSize: 30,
    scheduledDelayMillis: 5000,
    exportTimeoutMillis: ExportTimeoutMillis,
  });

  loggerProvider = new LoggerProvider({
    resource,
    processors: [logProcessor],
  });
  logs.setGlobalLoggerProvider(loggerProvider);

  console.debug(`[OTEL] Tracing and logging initialized for ${config.serviceName}`);
}

/**
 * Inject W3C Trace Context headers into a carrier object (e.g., HTTP request headers).
 *
 * This adds `traceparent` (and optionally `tracestate`) headers so the backend
 * can continue the trace started in the browser.
 */
export function injectTraceContext(headers: Record<string, string>): void {
  const setter: TextMapSetter<Record<string, string>> = {
    set(carrier, key, value) {
      carrier[key] = value;
    },
  };
  propagation.inject(context.active(), headers, setter);
}

/**
 * Get the IdentitySpanProcessor to update identity attributes after authentication.
 *
 * Returns null if OTel has not been initialized yet.
 */
export function getIdentitySpanProcessor(): IdentitySpanProcessor | null {
  return identityProcessor;
}

/**
 * Get a tracer instance for creating custom spans.
 */
export function getTracer(name: string): Tracer {
  return trace.getTracer(name);
}

/**
 * Gracefully shut down the tracer and logger providers, flushing pending data.
 */
export async function shutdownOtel(): Promise<void> {
  const shutdownPromises: Promise<void>[] = [];

  if (tracerProvider != null) {
    shutdownPromises.push(tracerProvider.shutdown());
    tracerProvider = null;
  }

  if (loggerProvider != null) {
    shutdownPromises.push(loggerProvider.shutdown());
    loggerProvider = null;
  }

  identityProcessor = null;

  await Promise.all(shutdownPromises);
}
