import { getKeycloakAccessTokenWithoutLogin } from "./keycloakTokenStore";

/**
 * The headers of every OTLP export of `initializeOtel`: the access token of the keycloak-js login, which otlp-gate
 * checks. A missing token or an initialization timeout yields no header, so the 401 of otlp-gate is the one failure path.
 */
export async function keycloakOtlpHeaders(): Promise<Record<string, string>> {
  try {
    const token = await getKeycloakAccessTokenWithoutLogin();
    return token === null ? {} : { Authorization: `Bearer ${token}` };
  } catch {
    return {};
  }
}
