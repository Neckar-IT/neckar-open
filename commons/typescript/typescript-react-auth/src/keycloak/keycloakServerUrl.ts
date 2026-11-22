/** Signs in every deployed frontend. */
const ProductionKeycloakServerUrl = "https://auth.neckar.it";

/** Signs in the local stack; reachable from the developer machine alone. */
const LocalKeycloakServerUrl = "https://auth.localdev";

/** Every host of the local stack ends on it. */
const LocalHostSuffix = ".localdev";

/**
 * The Keycloak server the frontend served on [hostname] signs in at, without a trailing slash: the local Keycloak for a host of the
 * local stack, the production Keycloak for every other host.
 */
export function keycloakServerUrl(hostname: string = window.location.hostname): string {
  return hostname.endsWith(LocalHostSuffix) ? LocalKeycloakServerUrl : ProductionKeycloakServerUrl;
}
