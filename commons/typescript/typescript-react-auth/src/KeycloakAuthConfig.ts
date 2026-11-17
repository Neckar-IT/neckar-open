import type { MeSessionConfig } from "./MeSessionConfig";

/**
 * Configuration for the KeycloakAuthProvider.
 */
export interface KeycloakAuthConfig extends MeSessionConfig {
  /**
   * The Keycloak server URL (e.g., `https://auth.neckar.it`).
   */
  url: string;

  /**
   * The Keycloak realm name (e.g., `internal`).
   */
  realm: string;

  /**
   * The Keycloak client ID registered for this application (e.g., `patterns.neckar.it`).
   * Must be a public client (no client secret) for SPA usage.
   */
  clientId: string;
}
