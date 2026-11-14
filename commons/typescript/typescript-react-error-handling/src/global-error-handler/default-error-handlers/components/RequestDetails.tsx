/**
 * Imports
 */

import { AxiosError } from "axios";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { formatErrorBody, MaxRequestBodyLength, MaxResponseBodyLength } from "../../formatErrorBody";
import { CollapsibleSection } from "./CollapsibleSection";

/**
 * Prop Types
 */

interface RequestDetailsProps {
  error: unknown;
  initiallyOpen?: boolean | undefined;
}

/**
 * Request-Details Component
 *
 * Surfaces the failed HTTP request (method, URL, status, response body, and an
 * optional short request body) so support and self-diagnosis do not require the
 * browser devtools. Renders nothing for non-axios errors.
 */

export function RequestDetails(props: RequestDetailsProps) {
  const { t } = useEmbeddedTranslation();

  if (!(props.error instanceof AxiosError)) {
    return null;
  }

  const error: AxiosError = props.error;
  const method = error.config?.method?.toUpperCase();
  const url = error.config?.url;
  const status = error.response?.status;
  const statusText = error.response?.statusText;
  const responseBody = formatErrorBody(error.response?.data, MaxResponseBodyLength);
  const requestBody = formatErrorBody(error.config?.data, MaxRequestBodyLength);

  return (
    <CollapsibleSection heading={t("requestDetails.heading")} initiallyOpen={props.initiallyOpen}>
      <div className="bg-muted w-full space-y-1 rounded-md border px-4 py-2 font-mono text-xs">
        {method !== undefined || url !== undefined ? (
          <Row label={t("requestDetails.endpoint")}>
            {method !== undefined ? `${method} ` : ""}
            {url ?? ""}
          </Row>
        ) : null}
        {status !== undefined ? (
          <Row label={t("requestDetails.status")}>
            {status}
            {statusText ? ` ${statusText}` : ""}
          </Row>
        ) : null}
        {responseBody !== undefined ? <BodyRow label={t("requestDetails.responseBody")} body={responseBody} /> : null}
        {requestBody !== undefined ? <BodyRow label={t("requestDetails.requestBody")} body={requestBody} /> : null}
      </div>
    </CollapsibleSection>
  );
}

/**
 * Single label/value row.
 */
function Row(props: { label: string; children: React.ReactNode }) {
  return (
    <dl className="flex gap-2">
      <dt className="text-muted-foreground shrink-0">{props.label}</dt>
      <dd className="break-all">{props.children}</dd>
    </dl>
  );
}

/**
 * Label above a preformatted, scrollable body block.
 */
function BodyRow(props: { label: string; body: string }) {
  return (
    <div>
      <div className="text-muted-foreground">{props.label}</div>
      <pre className="max-h-48 overflow-auto break-all whitespace-pre-wrap">{props.body}</pre>
    </div>
  );
}
