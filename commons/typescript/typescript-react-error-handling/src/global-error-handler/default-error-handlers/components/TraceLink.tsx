/**
 * Imports
 */

import { ExternalLinkIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { extractTraceId } from "../../extractTraceId";
import { buildTraceLinkUrl, useObservability } from "../../ObservabilityContext";

/**
 * Prop Types
 */

interface TraceLinkProps {
  error: unknown;
}

/**
 * Trace-Link Component
 *
 * Shows the OpenTelemetry trace ID of the failing request. When the app
 * supplies a Grafana-Tempo deep-link template, the trace ID links directly into
 * the Tempo trace-detail view; otherwise it is rendered as plain text so it can
 * still be copied. Renders nothing when no trace ID is available.
 */

export function TraceLink(props: TraceLinkProps) {
  const { t } = useEmbeddedTranslation();
  const observability = useObservability();

  const traceId = extractTraceId(props.error, observability?.getCurrentTraceId);
  if (traceId === undefined) {
    return null;
  }

  const traceLinkUrl = buildTraceLinkUrl(observability?.traceLinkTemplate, traceId);

  return (
    <div className="bg-muted flex w-full items-center gap-2 rounded-md border px-4 py-1 font-mono text-xs">
      <span className="text-muted-foreground font-semibold">{t("traceLink.heading")}</span>
      {traceLinkUrl !== undefined ? (
        <a
          className="text-primary inline-flex items-center gap-1 hover:underline"
          href={traceLinkUrl}
          rel="noopener noreferrer"
          target="_blank"
          title={t("traceLink.openInGrafana")}
        >
          <span>{traceId}</span>
          <ExternalLinkIcon className="h-3.5 w-3.5" />
        </a>
      ) : (
        <span>{traceId}</span>
      )}
    </div>
  );
}
