/**
 * Imports
 */

import { AxiosError } from "axios";
import { extractTraceId } from "../../extractTraceId";
import { formatErrorBody, MaxResponseBodyLength } from "../../formatErrorBody";
import { buildTraceLinkUrl, type ObservabilityConfig } from "../../ObservabilityContext";
import { type VersionInfoData } from "../../VersionInfoContext";

/**
 * Constants
 */

/**
 * The header carries at most this much of the message. The mail URL spends up to three characters
 * per UTF-8 byte, so the cap holds for umlauts and CJK text as well as for ASCII.
 */
const MaxHeaderMessageBytes = 200;

/**
 * Type Declarations
 */

/**
 * What an error page knows about its error.
 */
export interface ErrorInfoSource {
  errorMessage?: string | null;
  errorCaughtBy: string;
  errorCode?: string | null;
  exceptionType?: string | null;
  stackTrace?: string | null;
  /** The caught error. When it is an `AxiosError`, the text additionally includes the request details. */
  error?: unknown;
}

/**
 * Error Info Text
 */

/**
 * Builds the error information support receives by mail or clipboard. The short sections come
 * first, so a truncated support mail loses the long ones.
 */
export function buildErrorInfoText(
  params: ErrorInfoSource & {
    /** ISO-8601 timestamp of the error. */
    occurredAt: string;
    /** Hostname the deployment injected into the served HTML; `undefined` when nothing was injected. */
    serviceHost: string | undefined;
    versionInfo: VersionInfoData;
    observability: ObservabilityConfig | null;
  },
): string {
  const lines: string[] = [];

  // A long or multi-line message would push time, version and trace ID out of a truncated mail, so the
  // header carries its first line and the full text follows the short sections.
  const errorMessage = (params.errorMessage ?? "").replace(/\r\n?/g, "\n").trim();
  const headerMessage = utf8Prefix(errorMessage.split("\n")[0] ?? "", MaxHeaderMessageBytes);
  const messageFitsHeader = headerMessage === errorMessage;

  if (errorMessage) {
    lines.push(`Error Message: ${messageFitsHeader ? errorMessage : `${headerMessage} …`}`);
  }

  if (params.errorCode) {
    lines.push(`Error Code: ${params.errorCode}`);
  }

  if (params.exceptionType) {
    lines.push(`Exception Type: ${params.exceptionType}`);
  }

  lines.push(`Error Caught By: ${params.errorCaughtBy}`);
  lines.push(`Occurred At: ${params.occurredAt}`);
  // The subject carries the public URL; this names the deployment host whose logs support reads.
  lines.push(`Service Host: ${params.serviceHost ?? "(not injected)"}`);

  lines.push("");
  lines.push("Version Info:");
  lines.push(`Image Digest: ${params.versionInfo.imageDigest}`);

  // The Grafana URL is the longest of the short sections, so it closes them.
  const traceId = extractTraceId(params.error, params.observability?.getCurrentTraceId);
  if (traceId !== undefined) {
    lines.push("");
    lines.push(`Trace ID: ${traceId}`);
    const traceLinkUrl = buildTraceLinkUrl(params.observability?.traceLinkTemplate, traceId);
    if (traceLinkUrl !== undefined) {
      lines.push(`Grafana: ${traceLinkUrl}`);
    }
  }

  if (errorMessage && !messageFitsHeader) {
    lines.push("");
    lines.push("Full Error Message:");
    lines.push(errorMessage);
  }

  if (params.error instanceof AxiosError) {
    const method = params.error.config?.method?.toUpperCase();
    const url = params.error.config?.url;
    const status = params.error.response?.status;
    const statusText = params.error.response?.statusText;
    const responseBody = formatErrorBody(params.error.response?.data, MaxResponseBodyLength);

    lines.push("");
    lines.push("Request:");
    if (method !== undefined || url !== undefined) {
      lines.push(`${method !== undefined ? `${method} ` : ""}${url ?? ""}`.trim());
    }
    if (status !== undefined) {
      lines.push(`Status: ${status}${statusText ? ` ${statusText}` : ""}`);
    }
    if (responseBody !== undefined) {
      lines.push("Response Body:");
      lines.push(responseBody);
    }
  }

  if (params.stackTrace) {
    lines.push("");
    lines.push("Stack Trace:");
    lines.push(params.stackTrace);
  }

  return lines.join("\n");
}

/**
 * The longest prefix of whole code points that fits into `maxBytes` UTF-8 bytes.
 */
function utf8Prefix(text: string, maxBytes: number): string {
  const encoder = new TextEncoder();
  let bytes = 0;
  let prefix = "";
  for (const codePoint of text) {
    bytes += encoder.encode(codePoint).length;
    if (bytes > maxBytes) {
      return prefix;
    }
    prefix += codePoint;
  }
  return prefix;
}
