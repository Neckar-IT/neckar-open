import type { PropsWithChildren, ReactElement } from "react";
import { useMeSession } from "./hooks/useMeSession";
import { useProxyLogout } from "./hooks/useProxyLogout";
import { useSameOriginUnauthorized } from "./hooks/useSameOriginUnauthorized";
import type { ProxyAuthConfig } from "./ProxyAuthConfig";
import { MeSessionScreens } from "./screens/MeSessionScreens";

/**
 * Authentication for an application behind an OIDC middleware. It runs no login of its own — the
 * request that loaded the page already passed the middleware — and fetches only the identity.
 *
 * A same-origin 401 of a query or mutation shows the sign-in-again screen; the query client needs
 * `createQueryClient({ unauthorizedEndsSession: true })`, so that 401 is neither retried nor thrown.
 */
export function ProxyAuthProvider(props: PropsWithChildren<{ config: ProxyAuthConfig }>): ReactElement {
  const { config, children } = props;
  const { logout, isPending: logoutPending } = useProxyLogout();

  const meSession = useMeSession(config.onGetMe);
  const unauthorized = useSameOriginUnauthorized();
  const session = unauthorized ? { type: "sessionInvalid" as const } : meSession;

  return (
    <MeSessionScreens
      session={session}
      renderNotRegistered={config.renderNotRegistered}
      contactEmail={config.contactEmail}
      onLogout={logout}
      logoutPending={logoutPending}
      // The app holds no token to renew; a reload passes the middleware, and both traefik-oidc-auth and the
      // login gateway (/verify → /auth/login?returnTo=<path>) answer it with the login.
      onSignInAgain={() => window.location.reload()}
    >
      {children}
    </MeSessionScreens>
  );
}
