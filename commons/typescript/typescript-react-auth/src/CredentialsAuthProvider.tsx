import { LoadingIndicator } from "@neckar-hub/typescript-react-ui/feedback";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { AxiosError } from "axios";
import type { PropsWithChildren, ReactElement } from "react";
import { useState } from "react";
import { LogoutContextProvider } from "./context/LogoutContext";
import { UserContextProvider } from "./context/UserContext";
import type { CredentialsAuthConfig } from "./CredentialsAuthConfig";
import { useGetMeQuery } from "./hooks/useGetMeQuery";
import { useLoginMutation } from "./hooks/useLoginMutation";
import { useResetGetMeQueryOnClearRefreshToken } from "./hooks/useResetGetMeQueryOnClearRefreshToken";
import { useEmbeddedTranslation } from "./localization/useEmbeddedTranslation";
import { LoginScreen } from "./screens/LoginScreen";
import { LogoutAndReloadScreen } from "./screens/LogoutAndReloadScreen";
import { RegistrationScreen } from "./screens/RegistrationScreen";

type CredentialsAuthProviderProps = PropsWithChildren<{
  config: CredentialsAuthConfig;
}>;

export function CredentialsAuthProvider(props: CredentialsAuthProviderProps): ReactElement {
  return (
    <LogoutContextProvider onLogout={props.config.logout?.onLogout}>
      <CredentialsAuthProviderInner {...props} />
    </LogoutContextProvider>
  );
}

function CredentialsAuthProviderInner(props: CredentialsAuthProviderProps): ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeys = {
    authenticating: props.config.textKeys?.authenticating ?? "authProvider.authenticating",
  };

  const authStatus = useCredentialsAuth(props.config);

  useResetGetMeQueryOnClearRefreshToken();

  const pathname = window.location.pathname;

  // Show the registration screen if the url is "/register"
  if (props.config.userRegistration?.enabled && pathname === "/register") {
    return (
      <RegistrationScreen
        header={props.config.authScreenHeader}
        onRegister={props.config.userRegistration.onRegister}
        onRegisterSuccess={props.config.userRegistration.onRegisterSuccess}
        textKeys={props.config.userRegistration.textKeys}
      />
    );
  }

  // automatically log out the user on the `/logout` URL
  if (pathname === "/logout") {
    return <LogoutAndReloadScreen textKey={props.config.textKeys?.loggingOut} />;
  }

  switch (authStatus.type) {
    case "pending":
      return (
        <FullScreenCenter>
          <LoadingIndicator textKey={t(textKeys.authenticating)} />
        </FullScreenCenter>
      );

    case "not-authenticated":
      return (
        <LoginScreen
          header={props.config.authScreenHeader}
          footer={props.config.authScreenFooter}
          onLogin={props.config.login.onLogin}
          textKeys={props.config.login.textKeys}
        />
      );

    case "authenticated":
      return <UserContextProvider user={authStatus.user}>{props.children}</UserContextProvider>;
    default:
      throw new Error(`Unexpected value in Switch Statement: authStatus.type`);
  }
}

/**
 * Hook to handle the authentication logic
 */

function useCredentialsAuth(
  config: CredentialsAuthConfig,
): { type: "pending" } | { type: "authenticated"; user: UserDetails } | { type: "not-authenticated" } {
  const meResult = useGetMeQuery(config.onGetMe);
  const { mutate: login, isPending: autoLoginIsPending } = useLoginMutation(config.login.onLogin);

  const [autoLoginIsInvalid, setAutoLoginIsInvalid] = useState(false);

  const searchParams = new URLSearchParams(window.location.search);
  const autoLoginSearchParam = searchParams.get("auto-login");

  // show loading spinner while auto login is pending
  if (autoLoginIsPending) {
    return { type: "pending" };
  }

  // show login screen if the auto login failed
  if (autoLoginIsInvalid) {
    return { type: "not-authenticated" };
  }

  switch (meResult.status) {
    case "pending":
      return { type: "pending" };

    case "error": {
      const error = meResult.error;
      // rethrow error if not 401
      if (!(error instanceof AxiosError) || error.response?.status !== 401) {
        throw error;
      }

      // The query parameter is the second, independent trigger: it reaches the auto login on a
      // deployment whose `enabled` says no.
      if (config.autoLogin && (config.autoLogin.enabled || autoLoginSearchParam === "true")) {
        login(
          { loginName: config.autoLogin.loginName, password: config.autoLogin.password },
          {
            onSuccess: (response) => {
              if (response.type === "bad/InvalidCredentials" || response.type === "bad/UserDisabled") {
                setAutoLoginIsInvalid(true);
              }
            },
          },
        );
      }

      return { type: "not-authenticated" };
    }

    case "success": {
      const response = meResult.response;
      switch (response.type) {
        case "good":
          return { type: "authenticated", user: response.data };
        // Only services with an explicit registration flow emit this; treated as not-authenticated
        // instead of throwing inside the provider.
        case "bad/NotRegistered":
          return { type: "not-authenticated" };
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(response)}`);
      }
    }
    default:
      throw new Error(`Unexpected value in Switch Statement: meResult.status`);
  }
}
