import type Keycloak from "keycloak-js";
import { KeycloakReadyTimeoutMs } from "./keycloakTimeouts";
import { withTimeout } from "./withTimeout";

let keycloakInstance: Keycloak | null = null;
let sessionUnavailable = false;
let resolveReady: ((keycloak: Keycloak | null) => void) | null = null;
const readyPromise = new Promise<Keycloak | null>((resolve) => {
  resolveReady = resolve;
});

/**
 * Stores the Keycloak instance for use by the Axios interceptor.
 * Called by KeycloakAuthProvider after successful initialization.
 */
export function setKeycloakInstance(keycloak: Keycloak): void {
  keycloakInstance = keycloak;
  resolveReady?.(keycloak);
}

/**
 * Reports that initialization ended without a usable session — no SSO session, or `init()` failed.
 * Without it every request waits out [KeycloakReadyTimeoutMs] for a token that never arrives, which
 * fails even the unauthenticated ones.
 */
export function markKeycloakSessionUnavailable(): void {
  sessionUnavailable = true;
  resolveReady?.(null);
}

/**
 * Returns the current Keycloak access token, or null when no session is available. Refreshes a
 * token that expires within 30 seconds, and throws only when no outcome arrives within
 * [KeycloakReadyTimeoutMs] — an application that never mounted the provider.
 */
export async function getKeycloakAccessToken(): Promise<string | null> {
  const keycloak = await readyKeycloak();
  if (keycloak === null) {
    return null;
  }

  const token = await refreshedToken(keycloak);
  if (token === null) {
    void keycloak.login();
  }
  return token;
}

/**
 * [getKeycloakAccessToken] for a request in the background: a failed refresh yields null and keeps the page, so unsaved
 * input survives until the user acts again.
 */
export async function getKeycloakAccessTokenWithoutLogin(): Promise<string | null> {
  const keycloak = await readyKeycloak();
  return keycloak === null ? null : refreshedToken(keycloak);
}

/** The token of [keycloak], refreshed when it expires within 30 seconds; null when the refresh fails. */
async function refreshedToken(keycloak: Keycloak): Promise<string | null> {
  try {
    await keycloak.updateToken(30);
    return keycloak.token ?? null;
  } catch {
    return null;
  }
}

/** The initialized instance, null without a session; throws after [KeycloakReadyTimeoutMs] without an outcome. */
async function readyKeycloak(): Promise<Keycloak | null> {
  if (keycloakInstance === null) {
    if (sessionUnavailable) {
      return null;
    }

    keycloakInstance = await withTimeout(
      readyPromise,
      KeycloakReadyTimeoutMs,
      `Keycloak initialization timed out after ${String(KeycloakReadyTimeoutMs)}ms`,
    );
  }
  return keycloakInstance;
}
