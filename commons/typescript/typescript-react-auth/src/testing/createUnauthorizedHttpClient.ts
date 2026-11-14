/**
 * Imports
 */

import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";

/**
 * An axios instance whose adapter answers every request with HTTP 401, and the URLs it was asked for.
 */
export function createUnauthorizedHttpClient(): { httpClient: AxiosInstance; requests: string[] } {
  const requests: string[] = [];
  const httpClient = axios.create({
    adapter: (config: InternalAxiosRequestConfig) => {
      requests.push(axios.getUri(config));
      const response = { data: {}, status: 401, statusText: "Unauthorized", headers: {}, config };
      return Promise.reject(new AxiosError("unauthorized", AxiosError.ERR_BAD_REQUEST, config, undefined, response));
    },
  });
  return { httpClient, requests };
}
