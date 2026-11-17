import { Button } from "@neckar-hub/typescript-react-ui/actions";
import { LoadingIndicator } from "@neckar-hub/typescript-react-ui/feedback";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import { EMail } from "@neckar-hub/typescript-react-ui/text";
import { LogInIcon, LogOutIcon, MailIcon, ShieldXIcon, UserIcon, WifiOffIcon } from "lucide-react";
import type { ReactElement } from "react";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { FullScreenMessage, ReloadPageButton } from "./FullScreenMessage";

/**
 * Shown while the Keycloak login runs, before any identity exists. The spinner sets it apart from
 * {@link LoadingUserDataScreen}, which waits for a request rather than for a redirect.
 */
export function AuthenticatingScreen(): ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenCenter>
      <LoadingIndicator textKey={t("authProvider.authenticating")} />
    </FullScreenCenter>
  );
}

/**
 * The static `FullScreenMessage` layout tells a reload waiting for `GET /me` apart from
 * {@link AuthenticatingScreen}, whose spinner means the login itself is still running.
 */
export function LoadingUserDataScreen(): ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenMessage
      icon={<UserIcon className="h-10 w-10" />}
      title={t("authProvider.loadingUserData")}
      description={t("authProvider.loadingUserDataDescription")}
    />
  );
}

export function AuthServiceNotReachableScreen(): ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenMessage
      icon={<WifiOffIcon className="h-10 w-10" />}
      title={t("authProvider.authServiceNotReachable")}
      description={t("authProvider.authServiceNotReachableDescription")}
      action={<ReloadPageButton />}
    />
  );
}

export function LoginFailedScreen(): ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenMessage
      icon={<ShieldXIcon className="h-10 w-10" />}
      title={t("authProvider.loginFailed")}
      description={t("authProvider.loginFailedDescription")}
      action={<ReloadPageButton />}
    />
  );
}

/**
 * The identity is authenticated but has no access here yet. A reload cannot change that, so the
 * screen offers the two things that can: the product's `contactEmail`, and signing out.
 */
export function OnboardingRejectedScreen(props: {
  onLogout: () => void;
  /** Whether the logout is already on its way. Required: every caller owns a logout and its state. */
  logoutPending: boolean;
  contactEmail?: string;
}): ReactElement {
  const { onLogout, logoutPending, contactEmail } = props;
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenMessage
      icon={<MailIcon className="h-10 w-10" />}
      title={t("authProvider.onboardingRejected")}
      description={t("authProvider.onboardingRejectedDescription")}
      hint={
        contactEmail === undefined ? undefined : (
          <span className="max-w-72 text-center text-sm">
            {t("authProvider.onboardingRejectedContact")} <EMail value={contactEmail} />
          </span>
        )
      }
      action={
        <Button variant="outline" size="sm" disabled={logoutPending} onClick={onLogout}>
          <LogOutIcon className="mr-2 h-4 w-4" />
          {t("authProvider.signOut")}
        </Button>
      }
    />
  );
}

/**
 * Shown when the backend answers 401 — the session behind the request is gone or not accepted. The
 * navigation waits for the button: a 401 that a new login cannot fix would otherwise reload forever.
 */
export function SessionExpiredScreen(props: { onSignInAgain: () => void }): ReactElement {
  const { onSignInAgain } = props;
  const { t } = useEmbeddedTranslation();

  return (
    <FullScreenMessage
      icon={<ShieldXIcon className="h-10 w-10" />}
      title={t("authProvider.sessionExpired")}
      description={t("authProvider.sessionExpiredDescription")}
      action={
        <Button variant="outline" size="sm" onClick={onSignInAgain}>
          <LogInIcon className="mr-2 h-4 w-4" />
          {t("authProvider.signInAgain")}
        </Button>
      }
    />
  );
}
