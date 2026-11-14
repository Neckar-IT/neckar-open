import { type AxiosInstance, AxiosError } from "axios";
import { ImpersonationHeader, readImpersonatedUserId, stopImpersonation } from "./impersonationStorage";

/**
 * Prefix of every error code the backend answers a rejected impersonation with
 * (`it.neckar.user.management.impersonation.ImpersonationRejection`).
 */
const ImpersonationErrorCodePrefix = "IMPERSONATION-";

/**
 * Sends the user id this tab acts as in the [ImpersonationHeader] on every request of [instance], and
 * ends the impersonation once the backend rejects it — a revoked capability or a deleted target would
 * otherwise leave the tab failing on every request.
 */
export function registerImpersonationAxiosInterceptor(instance: AxiosInstance): void {
  instance.interceptors.request.use((config) => {
    const impersonatedUserId = readImpersonatedUserId();
    if (impersonatedUserId !== null) {
      config.headers.set(ImpersonationHeader, impersonatedUserId);
    }
    return config;
  });

  instance.interceptors.response.use(undefined, (error: unknown) => {
    if (readImpersonatedUserId() !== null && isImpersonationRejection(error)) {
      stopImpersonation();
    }
    return Promise.reject(error);
  });
}

function isImpersonationRejection(error: unknown): boolean {
  if (!(error instanceof AxiosError)) {
    return false;
  }

  const body: unknown = error.response?.data;
  if (typeof body !== "object" || body === null || !("errorCode" in body)) {
    return false;
  }

  return typeof body.errorCode === "string" && body.errorCode.startsWith(ImpersonationErrorCodePrefix);
}
