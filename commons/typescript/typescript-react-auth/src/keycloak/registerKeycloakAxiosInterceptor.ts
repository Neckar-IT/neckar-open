import type { AxiosInstance } from "axios";
import { getKeycloakAccessToken, getKeycloakAccessTokenWithoutLogin } from "./keycloakTokenStore";

declare module "axios" {
  interface AxiosRequestConfig {
    /** A request no user action started, such as the health poll: a failed token refresh keeps the page instead of logging in. */
    backgroundRequest?: boolean;
  }
}

/**
 * Registers an Axios request interceptor that attaches the Keycloak access token
 * as a Bearer Authorization header to every outgoing request.
 *
 * The token is refreshed automatically if it expires within 30 seconds.
 *
 * Call this once after creating the Axios instance — before React renders:
 * ```ts
 * const axiosInstance = axios.create();
 * registerKeycloakAxiosInterceptor(axiosInstance);
 * ```
 */
export function registerKeycloakAxiosInterceptor(axiosInstance: AxiosInstance): void {
  axiosInstance.interceptors.request.use(async (config) => {
    const token = config.backgroundRequest === true ? await getKeycloakAccessTokenWithoutLogin() : await getKeycloakAccessToken();
    if (token !== null) {
      config.headers.set("Authorization", `Bearer ${token}`);
    }
    return config;
  });
}
