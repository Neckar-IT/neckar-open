/**
 * Test/story fixture: builds an [AxiosError] with a response, config, and
 * optional `NeckarIT-Trace-Id` header — the shape the error handlers read for the
 * request-details and trace-link blocks. Not part of the public package API.
 */

import { PlatformHeader } from "@neckar-hub/typescript-utils/platform-headers";
import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";

export interface AxiosErrorFixtureOptions {
  status?: number;
  statusText?: string;
  method?: string;
  url?: string;
  responseData?: unknown;
  requestData?: unknown;
  traceId?: string;
}

export function makeAxiosError(options: AxiosErrorFixtureOptions = {}): AxiosError {
  const config = {
    method: options.method ?? "get",
    url: options.url ?? "/api/example",
    data: options.requestData,
    headers: {},
  } as InternalAxiosRequestConfig;

  const response =
    options.status === undefined
      ? undefined
      : ({
          status: options.status,
          statusText: options.statusText ?? "",
          data: options.responseData,
          headers: options.traceId !== undefined ? { [PlatformHeader.TraceId.toLowerCase()]: options.traceId } : {},
          config,
        } as AxiosResponse);

  return new AxiosError(
    `Request failed with status code ${options.status ?? "n/a"}`,
    options.status !== undefined && options.status >= 500 ? "ERR_BAD_RESPONSE" : "ERR_BAD_REQUEST",
    config,
    {},
    response,
  );
}
