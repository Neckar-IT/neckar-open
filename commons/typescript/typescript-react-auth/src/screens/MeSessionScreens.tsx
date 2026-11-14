/**
 * Imports
 */

import { outageToServiceAvailability } from "@neckar-hub/typescript-react-error-handling/app-connectivity";
import { ServiceUnavailablePage } from "@neckar-hub/typescript-react-error-handling/global-error-handler";
import { useQueryClient } from "@neckar-hub/typescript-react-query";
import { type PropsWithChildren, type ReactElement, useCallback } from "react";
import { ImpersonationBanner } from "../components/ImpersonationBanner";
import { UserContextProvider } from "../context/UserContext";
import { getMeQueryKey } from "../hooks/useGetMeQuery";
import type { MeSession } from "../hooks/useMeSession";
import { readImpersonatedUserId } from "../impersonation/impersonationStorage";
import type { NotRegisteredContext } from "../NotRegisteredContext";
import { LoadingUserDataScreen, OnboardingRejectedScreen, SessionExpiredScreen } from "./AuthScreens";

/**
 * The screen each session state calls for, and the application below a registered one. Both auth
 * providers render it: they differ in how the session is established and ended, not in what the
 * user is shown while it is not usable.
 */
export function MeSessionScreens(
  props: PropsWithChildren<{
    session: MeSession;
    renderNotRegistered?: (context: NotRegisteredContext) => ReactElement;
    contactEmail?: string;
    onLogout: () => void;
    logoutPending: boolean;
    /** Starts a new login. A reload reaches the middleware; a provider with its own login calls it. */
    onSignInAgain: () => void;
  }>,
): ReactElement {
  const { session, renderNotRegistered, contactEmail, onLogout, logoutPending, onSignInAgain, children } = props;
  const queryClient = useQueryClient();
  const retryGetMe = useCallback(() => void queryClient.invalidateQueries({ queryKey: getMeQueryKey }), [queryClient]);

  switch (session.type) {
    case "loading":
      return <LoadingUserDataScreen />;

    // `ServiceUnavailablePage` shows the polled health state once the poll answers; useMeSession polls /me meanwhile.
    case "serviceUnavailable":
      return (
        <ServiceUnavailablePage
          fallbackAvailability={outageToServiceAvailability(session.failure)}
          onRecover={retryGetMe}
          fetchesOnItsOwn={true}
        />
      );

    case "failed":
      // The error boundary above renders `UnexpectedErrorPage`, `PermissionDeniedPage` or the version mismatch page, as for a query without data.
      throw session.error;

    case "sessionInvalid":
      return <SessionExpiredScreen onSignInAgain={onSignInAgain} />;

    // Business-level outcome, delivered with HTTP 200: authenticated, but no registered account in
    // this service — the product renders its registration flow.
    case "notRegistered":
      return renderNotRegistered !== undefined ? (
        renderNotRegistered({
          selfSignupAllowed: session.selfSignupAllowed,
          onRegistered: retryGetMe,
          contactEmail,
          onLogout,
          logoutPending,
        })
      ) : (
        <OnboardingRejectedScreen onLogout={onLogout} logoutPending={logoutPending} contactEmail={contactEmail} />
      );

    case "registered": {
      const impersonatedUserId = readImpersonatedUserId();
      // The backend answers an impersonating request as the named user or rejects it, so any other user here is a defect.
      if (impersonatedUserId !== null && impersonatedUserId !== session.user.id) {
        throw new Error(`Tab impersonates user ${impersonatedUserId}, but the backend answered as user ${session.user.id}`);
      }

      return (
        <UserContextProvider user={session.user}>
          {children}
          {impersonatedUserId !== null && <ImpersonationBanner user={session.user} />}
        </UserContextProvider>
      );
    }

    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(session)}`);
  }
}
