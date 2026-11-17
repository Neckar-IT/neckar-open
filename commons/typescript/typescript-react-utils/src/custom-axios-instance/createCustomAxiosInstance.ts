/**
 * Use this function to create a custom axios instance.
 *
 * The axios instance is then use to create the httpClient hat is in turn used by orvaal to call the backend functions
 * that it generates from the openapi schema.
 *
 * Features:
 * - Add a client and request id to the request headers
 * - Add the access token to the request header
 * - Try to obtain new access and refresh tokens if the request fails with 401
 * - Replace ISO date strings with Date objects in the response
 *
 * Usage:
 *
 * const axiosInstance = createCustomAxiosInstance({...});
 *
 * export const httpClient = async <T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<T> => {
 *   const response = await axiosInstance({ ...config, ...options });
 *   return response.data;
 * };
 *
 * Unfortunately, we cannot create the httpClient directly since orval parses the file with the httpClient and expects
 * a function call similar to the one above.
 */

/**
 * Imports
 */

import { generateNanoId } from "@neckar-hub/typescript-utils/generators";
import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { type JwtRefreshTokenResponse, type JwtTokenPair } from "@neckar-hub/typescript-utils/openapi-types";
import { PlatformHeader } from "@neckar-hub/typescript-utils/platform-headers";
import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from "axios";

/**
 * Ktor-native header — not part of `PlatformHeader`. Kept as a local literal because Ktor
 * generates and reads it through its own `HttpHeaders.XRequestId` constant.
 */
const REQUEST_ID_HEADER = "X-Request-Id";
import { getClientId } from "./clientId";
import { HttpStatusCodes } from "./HttpStatusCodes";
import { clearTokenPair, getAccessToken, getRefreshToken, setTokenPair } from "./jwtTokenPair.store";

/**
 * Type Declarations
 */

export interface CreateCustomAxiosInstanceOptions {
  baseUrl?: string;
  onRefreshTokenPair: (options?: AxiosRequestConfig) => Promise<JwtRefreshTokenResponse>;
  unauthenticatedRequestUrls: string[];
  featureFlags?: string | null | undefined;
  /**
   * Commit of a frontend that names it, sent as `NeckarIT-Protocol-Version` to a service that compares it with its own;
   * without it the request states no version, which the protocol-version check accepts.
   */
  gitHash?: string;
}

/**
 * Create HTTP Client
 *
 * @param options
 * @param options.baseUrl - Base URL
 * @param options.onRefreshTokenPair - Function to refresh token pair or throw an error
 * @param options.unauthenticatedRequestUrls - List of URLs that can be called without a token
 * @param options.featureFlags - Feature flags sent as NeckarIT-Feature-Flags header
 * @param options.gitHash - Commit sent as NeckarIT-Protocol-Version header, if given
 */

export function createCustomAxiosInstance({
  baseUrl,
  onRefreshTokenPair,
  unauthenticatedRequestUrls,
  featureFlags,
  gitHash,
}: CreateCustomAxiosInstanceOptions) {
  const axiosInstance = axios.create({
    baseURL: baseUrl,
    //TODO check, if this is necessary for all calls?
    //Needed at least for login + refresh token
    withCredentials: true,
    headers: {
      Accept: "application/json",
    },
  });

  /**
   * Intercept Request to set headers
   */
  axiosInstance.interceptors.request.use((config) => {
    // add access token to the request header if not set already
    if (isNotDefined(config.headers.Authorization)) {
      const accessToken = getAccessToken();
      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
      }
    }

    // add client id and request id to the request header
    const clientId = getClientId();
    const requestId = generateNanoId(10);

    if (gitHash !== undefined) {
      config.headers[PlatformHeader.ProtocolVersion] = gitHash;
    }

    config.headers[PlatformHeader.ClientId] = clientId;
    config.headers[REQUEST_ID_HEADER] = requestId;

    // set feature flags in the header
    if (featureFlags) {
      config.headers[PlatformHeader.FeatureFlags] = featureFlags;
    }

    return config;
  });

  /**
   * Intercept Response to replace ISO date strings with Date objects and try to refesh token pair if the request
   * fails with 401
   */
  axiosInstance.interceptors.response.use(
    // successful request
    (response) => {
      return response;
    },
    // request failed => try to refresh token pair if the status code is 401
    async (error) => {
      const originalRequest = error.config;
      if (
        error.response?.status !== HttpStatusCodes.Unauthorized ||
        originalRequest._retry === true ||
        unauthenticatedRequestUrls.includes(originalRequest.url)
      ) {
        throw error;
      }

      console.debug("[Auth] Access token expired, attempting refresh for:", originalRequest.url);

      // Read the refresh token once — used for both the null check and the refresh request.
      const refreshToken = getRefreshToken();

      // No refresh token available — cannot attempt refresh, propagate the 401.
      // This is normal when the user has not logged in yet (e.g., first page visit).
      if (refreshToken == null) {
        console.debug("[Auth] No refresh token available, not attempting refresh");
        throw error;
      }

      // Refresh must block here: otherwise the original request would fail before the tokens are refreshed and retried.
      try {
        const tokenPair = await refreshTokens(onRefreshTokenPair, refreshToken);
        console.debug("[Auth] Token refresh successful, retrying original request");
        originalRequest._retry = true;
        originalRequest.headers.Authorization = `Bearer ${tokenPair.accessToken}`;
        return await axiosInstance(originalRequest);
      } catch (refreshError) {
        // Only a definitive rejection by the auth server ends the session.
        // Transient failures (network error, 5xx) keep the token pair — the next 401 retries the refresh.
        if (isDefinitiveAuthRejection(refreshError)) {
          console.warn("[Auth] Token refresh rejected by auth server, clearing session");
          clearTokenPair();
        } else {
          console.warn("[Auth] Token refresh failed transiently, keeping session for retry");
        }
        throw refreshError;
      }
    },
  );

  /**
   * Return the Axios Instance
   */
  return axiosInstance;
}

/**
 * Determines whether a refresh failure is a definitive rejection by the auth server
 * (HTTP 401/403) as opposed to a transient failure (network error, 5xx).
 * Only definitive rejections may end the session.
 */
function isDefinitiveAuthRejection(error: unknown): boolean {
  if (!axios.isAxiosError(error)) {
    return false;
  }
  switch (error.response?.status) {
    case HttpStatusCodes.Unauthorized:
    case HttpStatusCodes.Forbidden:
      return true;
    case undefined:
    default:
      return false;
  }
}

/**
 * Refresh Tokens
 */

let refreshTokensPromise: Promise<JwtTokenPair> | null = null;

async function refreshTokens(
  onRefreshTokenPair: (options?: AxiosRequestConfig) => Promise<JwtRefreshTokenResponse>,
  refreshToken: string,
): Promise<JwtTokenPair> {
  if (refreshTokensPromise != null) {
    console.debug("[Auth] Refresh already in progress, reusing existing request");
    return refreshTokensPromise;
  }

  const headers = new AxiosHeaders();
  headers.set("Authorization", `Bearer ${refreshToken}`);

  console.debug("[Auth] Sending refresh request with refresh token in Authorization header");

  const refreshTokenResponsePromise = onRefreshTokenPair({ headers })
    .then((response) => {
      switch (response.type) {
        case "good":
          console.debug("[Auth] Refresh response: good");
          return response.data;
        case "bad/InvalidRefreshToken":
          console.warn("[Auth] Refresh response: bad/InvalidRefreshToken (server rejected refresh token)");
          clearTokenPair();
          throw new AxiosError("Unauthorized", AxiosError.ERR_BAD_REQUEST, undefined, null, {
            status: 401,
            statusText: "Unauthorized",
            headers: {},
            config: { headers },
            data: null,
          });
        default:
          throw new Error(`Unexpected value in Switch Statement: response.type`);
      }
    })
    .then((jwtTokenPair) => {
      setTokenPair(jwtTokenPair);
      return jwtTokenPair;
    })
    .finally(() => {
      refreshTokensPromise = null;
    });

  refreshTokensPromise = refreshTokenResponsePromise;
  return refreshTokensPromise;
}
