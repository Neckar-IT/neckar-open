/**
 * Imports
 */

import { createContext, useContext, type PropsWithChildren } from "react";

/**
 * Type Declarations
 */

/**
 * Placeholder substituted with the concrete trace ID inside
 * [ObservabilityConfig.traceLinkTemplate].
 */
export const TraceIdPlaceholder = "__TRACE_ID__";

export interface ObservabilityConfig {
  /**
   * Full Grafana-Tempo deep-link URL template with the literal placeholder
   * [TraceIdPlaceholder] where the concrete trace ID is substituted.
   *
   * Built by the app config from the environment-specific Grafana base URL, so
   * the version-dependent Grafana Explore URL schema lives in exactly one place
   * outside this library. When `undefined`, the trace ID is shown as plain text
   * without a link.
   */
  traceLinkTemplate?: string;

  /**
   * Optional getter for a locally generated trace ID, used for errors that
   * never reached the backend (no `NeckarIT-Trace-Id` response header). Injected by
   * apps that run the frontend OpenTelemetry SDK, so this library never imports
   * `@opentelemetry/api` directly.
   */
  getCurrentTraceId?: () => string | undefined;
}

/**
 * Context
 */

const ObservabilityContext = createContext<ObservabilityConfig | null>(null);

/**
 * Provider
 *
 * Always rendered by [GlobalErrorHandler]; when the app passes no
 * `observability` prop the value is `null` and the error page falls back to
 * plain-text trace IDs.
 */

export function ObservabilityProvider(props: PropsWithChildren<{ observability: ObservabilityConfig | undefined }>) {
  return <ObservabilityContext.Provider value={props.observability ?? null}>{props.children}</ObservabilityContext.Provider>;
}

/**
 * Hook
 *
 * Non-throwing: returns `null` when no [ObservabilityProvider] is present or the
 * app supplied no config, so error handlers can render outside a configured
 * observability context.
 */

export function useObservability(): ObservabilityConfig | null {
  return useContext(ObservabilityContext);
}

/**
 * Substitutes the trace ID into the deep-link template. Returns `undefined`
 * when no template is configured.
 */
export function buildTraceLinkUrl(traceLinkTemplate: string | undefined, traceId: string): string | undefined {
  if (traceLinkTemplate === undefined) {
    return undefined;
  }
  return traceLinkTemplate.replaceAll(TraceIdPlaceholder, encodeURIComponent(traceId));
}
