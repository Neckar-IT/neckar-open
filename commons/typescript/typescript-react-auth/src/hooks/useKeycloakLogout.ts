/**
 * Imports
 */

import { useCallback, useState } from "react";
import { useKeycloak } from "../keycloak/KeycloakContext";

/**
 * Triggers an OIDC RP-Initiated Logout: redirects to Keycloak's `end_session_endpoint`,
 * which clears the SSO cookie and returns to `redirectUri`. `KeycloakAuthProvider`
 * then finds no session at `check-sso` and calls `login()`. Without
 * this server-side round-trip the SSO cookie would survive and silently re-authenticate.
 *
 * `isPending` guards against double-clicks before the navigation starts as well as getting stuck after a failed logout attempt. It is reset
 * only on rejection (e.g. Keycloak unreachable); on success the page navigates away.
 *
 * @param redirectUri Target after logout. Must match the Keycloak client's "Valid post
 *   logout redirect URIs" — otherwise Keycloak shows an "Invalid redirect uri" page and
 *   the SSO session is NOT terminated. Defaults to `window.location.origin` (no path);
 *   we have decided to override the keycloak-js fallback `window.location.href` with origin to
 *   1) avoid leaking credentials post logout
 *   2) have the next user logging in start at the app's root instead of at the last visited page (possibly by someone else).
 *   3) avoid having to explicitly whitelist url WITH path and query params in the Keycloak client configuration.
 */
export function useKeycloakLogout(redirectUri: string | undefined = window.location.origin): { logout: () => void; isPending: boolean } {
  const keycloak = useKeycloak();
  const [isPending, setIsPending] = useState(false);

  const logout = useCallback(() => {
    setIsPending(true);
    keycloak.logout({ redirectUri }).catch(() => {
      setIsPending(false);
    });
  }, [keycloak, redirectUri]);

  return { logout, isPending };
}
