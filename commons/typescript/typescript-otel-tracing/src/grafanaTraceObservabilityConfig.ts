import { type ObservabilityConfig, TraceIdPlaceholder } from "@neckar-hub/typescript-utils/observability";

/**
 * The only place that knows the Grafana Explore URL schema (`panes=`, Grafana 13) and the Tempo datasource UID `tempo`,
 * so a Grafana upgrade changes this file alone. Vite replaces `import.meta.env.PROD` when it builds the app.
 */
export function grafanaTraceObservabilityConfig(): ObservabilityConfig {
  const grafanaBaseUrl = import.meta.env.PROD ? "https://monitoring.neckar.it" : "https://monitoring.localdev";
  const traceExplorePanes = {
    trace: {
      datasource: "tempo",
      queries: [{ refId: "A", datasource: { type: "tempo", uid: "tempo" }, queryType: "traceql", query: TraceIdPlaceholder }],
      range: { from: "now-1h", to: "now" },
    },
  };
  return {
    traceLinkTemplate: `${grafanaBaseUrl}/explore?schemaVersion=1&orgId=1&panes=${encodeURIComponent(JSON.stringify(traceExplorePanes))}`,
  };
}
