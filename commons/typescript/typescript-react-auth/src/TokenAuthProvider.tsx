/**
 * Imports
 */

import { ErrorIndicator, LoadingIndicator } from "@neckar-hub/typescript-react-ui/feedback";
import { FullScreenCenter } from "@neckar-hub/typescript-react-ui/layout";
import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { AxiosError } from "axios";
import type { PropsWithChildren, ReactElement } from "react";
import { useState } from "react";
import { LogoutContextProvider } from "./context/LogoutContext";
import { UserContextProvider } from "./context/UserContext";
import { useGetMeQuery } from "./hooks/useGetMeQuery";
import { useLoginWithTokenMutation } from "./hooks/useLoginWithTokenMutation";
import { useResetGetMeQueryOnClearRefreshToken } from "./hooks/useResetGetMeQueryOnClearRefreshToken";
import { useEmbeddedTranslation } from "./localization/useEmbeddedTranslation";
import type { TokenAuthConfig } from "./TokenAuthConfig";

/**
 * Prop Types
 */

type TokenAuthProviderProps = PropsWithChildren<{
  config: TokenAuthConfig;
}>;

/**
 * Token Authentication Provider
 *
 * authentication process with login token:
 * 1.   try to get the logged in user by using the access token from local storage (the mutation startes in idle state)
 *      and show a loading screen while fetching the user
 * 2.1  user was fetch successfully => show the app
 * 2.2  fetching the user failed with 401 => start the login with token mutation and show a loading screen
 * 3.1  login with token was successful => rerun the query to get the user and show the app
 * 3.2  login with token failed => show an error message
 */

export function TokenAuthProvider(props: TokenAuthProviderProps): ReactElement {
  return (
    <LogoutContextProvider onLogout={props.config.logout?.onLogout}>
      <TokenAuthProviderInner {...props} />
    </LogoutContextProvider>
  );
}

function TokenAuthProviderInner(props: TokenAuthProviderProps): ReactElement {
  const { t } = useEmbeddedTranslation();
  const textKeys = {
    authenticating: props.config.textKeys?.authenticating ?? "authProvider.authenticating",
    invalidLoginToken: props.config.login.textKeys?.invalidLoginToken ?? "authProvider.invalidLoginToken",
  };

  const authStatus = useTokenAuth(props.config);

  useResetGetMeQueryOnClearRefreshToken();

  switch (authStatus.type) {
    case "pending":
      return (
        <FullScreenCenter>
          <LoadingIndicator textKey={t(textKeys.authenticating)} />
        </FullScreenCenter>
      );

    case "invalid-token":
      return (
        <FullScreenCenter>
          <ErrorIndicator textKey={t(textKeys.invalidLoginToken)} />
        </FullScreenCenter>
      );

    case "authenticated":
      return <UserContextProvider user={authStatus.user}>{props.children}</UserContextProvider>;
    default:
      throw new Error(`Unexpected value in Switch Statement: authStatus.type`);
  }
}

/**
 * Hook to handle the token authentication logic
 */

function useTokenAuth(
  config: TokenAuthConfig,
): { type: "pending" } | { type: "authenticated"; user: UserDetails } | { type: "invalid-token" } {
  const meResult = useGetMeQuery(config.onGetMe);
  const { mutate: loginWithToken, isPending } = useLoginWithTokenMutation(config.login.onLoginWithToken);

  const [tokenIsInvalid, setTokenIsInvalid] = useState(false);

  const searchParams = new URLSearchParams(window.location.search);
  const token = searchParams.get("token");

  if (token === null || tokenIsInvalid) {
    return { type: "invalid-token" };
  }

  if (isPending) {
    return { type: "pending" };
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

      // try to login with token
      loginWithToken(
        { token },
        {
          onSuccess: (response) => {
            if (response.type === "bad/InvalidCredentials" || response.type === "bad/UserDisabled") {
              setTokenIsInvalid(true);
            }
          },
        },
      );

      return { type: "pending" };
    }

    case "success": {
      const response = meResult.response;
      switch (response.type) {
        case "good":
          return { type: "authenticated", user: response.data };
        // Only services with an explicit registration flow emit this; treated as invalid token
        // instead of throwing inside the provider.
        case "bad/NotRegistered":
          return { type: "invalid-token" };
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(response)}`);
      }
    }
    default:
      throw new Error(`Unexpected value in Switch Statement: meResult.status`);
  }
}
