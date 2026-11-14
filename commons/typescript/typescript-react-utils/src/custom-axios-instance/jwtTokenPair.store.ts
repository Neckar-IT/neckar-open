/**
 * Imports
 */

import { type JwtTokenPair } from "@neckar-hub/typescript-utils/openapi-types";
import { useAtomValue } from "jotai";
import { atomWithLocalStorage, jotaiStore } from "../jotai";

/**
 * Constants
 */

const ACCESS_TOKEN = "access-token";
const REFRESH_TOKEN = "refresh-token";

/**
 * JWT
 */

const accessTokenAtom = atomWithLocalStorage<string | null>(ACCESS_TOKEN, null);
const refreshTokenAtom = atomWithLocalStorage<string | null>(REFRESH_TOKEN, null);

/**
 * Getters
 */

export function getAccessToken() {
  return jotaiStore.get(accessTokenAtom);
}

export function getRefreshToken() {
  return jotaiStore.get(refreshTokenAtom);
}

/**
 * Actions
 */

export function setTokenPair({ accessToken, refreshToken }: JwtTokenPair) {
  jotaiStore.set(accessTokenAtom, accessToken);
  jotaiStore.set(refreshTokenAtom, refreshToken);
}

export function clearTokenPair() {
  jotaiStore.set(accessTokenAtom, null);
  jotaiStore.set(refreshTokenAtom, null);
}

/**
 * Hook to get the refresh token value
 */

export function useRefreshToken() {
  const refreshToken = useAtomValue(refreshTokenAtom);
  return refreshToken;
}
