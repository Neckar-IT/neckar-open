/**
 * Imports
 */

import { reportClientError } from "@neckar-hub/typescript-otel-tracing";
import { FeatureFlag, FeatureFlagsSupport } from "@neckar-hub/typescript-utils/feature-flags";
import Keycloak from "keycloak-js";
import type { PropsWithChildren, ReactElement } from "react";
import { useEffect, useRef, useState } from "react";
import { useKeycloakLogout } from "./hooks/useKeycloakLogout";
import { useMeSession } from "./hooks/useMeSession";
import { KeycloakContext, useKeycloak } from "./keycloak/KeycloakContext";
import { KeycloakInitTimeoutMs } from "./keycloak/keycloakTimeouts";
import { markKeycloakSessionUnavailable, setKeycloakInstance } from "./keycloak/keycloakTokenStore";
import { withTimeout } from "./keycloak/withTimeout";
import type { KeycloakAuthConfig } from "./KeycloakAuthConfig";
import { AuthenticatingScreen, AuthServiceNotReachableScreen, LoginFailedScreen } from "./screens/AuthScreens";
import { MeSessionScreens } from "./screens/MeSessionScreens";

/**
 * Prop Types
 */

type KeycloakAuthProviderProps = PropsWithChildren<{
  config: KeycloakAuthConfig;
}>;

/**
 * Keycloak Authentication Provider
 *
 * Initializes Keycloak with `check-sso` mode and a silent-check-sso iframe: an
 * existing SSO session is picked up silently, inside a hidden iframe, without a
 * top-level redirect — so an authenticated page reload boots the app only once
 * instead of twice. Only when no session exists does the provider fall back to
 * an interactive `login()` redirect.
 *
 * The silent check requires a `silent-check-sso.html` file served at the app's
 * own origin (referenced via `silentCheckSsoRedirectUri`), and that URL must be
 * registered as a valid redirect URI on the Keycloak client.
 *
 * After authentication the access token is attached to API requests via an
 * Axios interceptor. Token refresh is handled automatically by keycloak-js.
 */
export function KeycloakAuthProvider(props: KeycloakAuthProviderProps): ReactElement {
  const { config, children } = props;

  const [keycloak, setKeycloak] = useState<Keycloak | null>(null);
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [initError, setInitError] = useState(false);
  const initStarted = useRef(false);

  useEffect(() => {
    // Guard against React 18 Strict Mode double-invocation.
    // keycloak-js exchanges the authorization code on init() — a second call
    // with the same code fails with 401 because codes are single-use.
    if (initStarted.current) {
      return;
    }
    initStarted.current = true;

    const simulateKeycloakDown = FeatureFlagsSupport.active(new FeatureFlag("simulateKeycloakDown"));
    const simulateKeycloakSlow = FeatureFlagsSupport.active(new FeatureFlag("simulateKeycloakSlow"));

    // simulateKeycloakDown replaces the Keycloak URL with 502.neckar.it,
    // a Traefik route on hafen-host.neckar.it that always returns HTTP 502.
    const kc = new Keycloak({
      url: simulateKeycloakDown ? "https://502.neckar.it" : config.url,
      realm: config.realm,
      clientId: config.clientId,
    });

    const initKeycloak = () => {
      // Diagnostic logging paired with the equivalent GET /me logs below — lets
      // you see in the browser console which auth phase dominates on a reload.
      const startedAt = performance.now();
      console.info("[Auth] Keycloak init: start");
      void (async () => {
        try {
          const isAuthenticated = await withTimeout(
            kc.init({
              onLoad: "check-sso",
              silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
              pkceMethod: "S256",
              checkLoginIframe: false,
            }),
            KeycloakInitTimeoutMs,
            `keycloak.init timed out after ${KeycloakInitTimeoutMs} ms`,
          );
          const durationMs = Math.round(performance.now() - startedAt);
          console.info(`[Auth] Keycloak init: done in ${durationMs}ms (authenticated: ${isAuthenticated})`);
          if (!isAuthenticated) {
            // No existing SSO session: redirect to the Keycloak login form.
            // The silent check-sso iframe above already picked up an existing
            // session without a redirect, so this path is hit only for genuinely
            // unauthenticated users — never on an authenticated reload.
            markKeycloakSessionUnavailable();
            await kc.login();
            return;
          }
          setKeycloakInstance(kc);
          setKeycloak(kc);
          setAuthenticated(isAuthenticated);
          setInitialized(true);
        } catch (error: unknown) {
          const durationMs = Math.round(performance.now() - startedAt);
          console.error(`[Auth] Keycloak init: failed after ${durationMs}ms`, error);
          reportClientError("AuthInitFailed", error);
          markKeycloakSessionUnavailable();
          setInitialized(true);
          setAuthenticated(false);
          setInitError(true);
        }
      })();
    };

    // Only delay on the initial page load, not on the Keycloak callback (which has state= in the fragment).
    const isKeycloakCallback = window.location.hash.includes("state=");
    if (simulateKeycloakSlow && !isKeycloakCallback) {
      setTimeout(initKeycloak, 5_000);
    } else {
      initKeycloak();
    }
    // Only initialize once — config values are static
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!initialized || keycloak === null) {
    if (initError) {
      return <AuthServiceNotReachableScreen />;
    }

    return <AuthenticatingScreen />;
  }

  if (!authenticated) {
    return <LoginFailedScreen />;
  }

  return (
    <KeycloakContext value={keycloak}>
      <KeycloakUserProvider config={config}>{children}</KeycloakUserProvider>
    </KeycloakContext>
  );
}

/**
 * Inner component that fetches the user from the backend after Keycloak authentication.
 */
function KeycloakUserProvider(props: PropsWithChildren<{ config: KeycloakAuthConfig }>): ReactElement {
  const { config, children } = props;
  const { logout, isPending: logoutPending } = useKeycloakLogout();
  const keycloak = useKeycloak();

  const session = useMeSession(config.onGetMe);

  return (
    <MeSessionScreens
      session={session}
      renderNotRegistered={config.renderNotRegistered}
      contactEmail={config.contactEmail}
      onLogout={logout}
      logoutPending={logoutPending}
      // A reload would pick the same SSO session back up and land on the same 401.
      onSignInAgain={() => void keycloak.login()}
    >
      {children}
    </MeSessionScreens>
  );
}
