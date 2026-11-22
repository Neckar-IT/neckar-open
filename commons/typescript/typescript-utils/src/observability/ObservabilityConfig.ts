export const TraceIdPlaceholder = "__TRACE_ID__";

export interface ObservabilityConfig {
  /**
   * Grafana-Tempo deep link with [TraceIdPlaceholder] in place of the trace ID, built by `grafanaTraceObservabilityConfig()`;
   * `undefined` shows the trace ID as plain text.
   */
  traceLinkTemplate?: string;

  /**
   * Trace ID for an error without `NeckarIT-Trace-Id` response header; a getter keeps `@opentelemetry/api` out of the
   * error UI.
   */
  getCurrentTraceId?: () => string | undefined;
}
