/**
 * Imports
 */

import { type ObservabilityConfig, TraceIdPlaceholder } from "@neckar-hub/typescript-utils/observability";
import { createContext, useContext, type PropsWithChildren } from "react";

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
