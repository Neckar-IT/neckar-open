/**
 * Imports
 */

import type Keycloak from "keycloak-js";
import { createContext, useContext } from "react";

/**
 * Context to provide the Keycloak instance to child components.
 * Allows access to the token, logout, and other Keycloak functions.
 *
 * Its own module because `useKeycloakLogout` reads it while `KeycloakAuthProvider` renders that
 * hook — sharing a file would close an import cycle.
 */
export const KeycloakContext = createContext<Keycloak | undefined>(undefined);

/**
 * Returns the Keycloak instance from context.
 */
export function useKeycloak(): Keycloak {
  const keycloak = useContext(KeycloakContext);
  if (keycloak === undefined) {
    throw new Error("`useKeycloak` must be used within a KeycloakAuthProvider");
  }
  return keycloak;
}
