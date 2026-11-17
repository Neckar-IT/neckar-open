import { isAxiosError, isCancel } from "axios";

/**
 * Whether [error] is a canceled request or an HTTP 4xx: a client-side expectation, not a frontend bug.
 * The Axios tracing interceptor records a 4xx as a failed span already.
 */
export function isExpectedError(error: unknown): boolean {
  const httpStatus = httpResponseStatus(error);
  return isCancel(error) || (httpStatus != null && httpStatus < 500);
}

/** `null` for an error without an Axios response, e.g. a network error. */
export function httpResponseStatus(error: unknown): number | null {
  if (!isAxiosError(error) || error.response == null) {
    return null;
  }
  const status = error.response.status;
  return typeof status === "number" ? status : null;
}
