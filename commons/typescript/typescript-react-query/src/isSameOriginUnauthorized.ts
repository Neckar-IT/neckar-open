/**
 * Imports
 */

import axios, { isAxiosError } from "axios";

/**
 * Whether [error] is an HTTP 401 the page's own origin answered: the session behind the page ended.
 * A 401 of a foreign origin says nothing about that session and stays an ordinary error.
 */
export function isSameOriginUnauthorized(error: unknown): boolean {
  if (!isAxiosError(error) || error.response?.status !== 401) {
    return false;
  }

  const config = error.config ?? error.response.config;
  const requestUrl = new URL(axios.getUri(config), window.location.href);
  return requestUrl.origin === window.location.origin;
}
