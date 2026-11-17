/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type { LoginResponse, LoginWithTokenRequest, MeResponse } from "@neckar-hub/typescript-utils/openapi-types";

/**
 * Define the configuration for the TokenAuthProvider
 */

export interface TokenAuthConfig {
  onGetMe: () => Promise<MeResponse>;
  textKeys?: {
    authenticating?: TextKey;
  };
  login: {
    onLoginWithToken: (loginRequest: LoginWithTokenRequest) => Promise<LoginResponse>;
    textKeys?: {
      invalidLoginToken?: TextKey;
    };
  };
  logout?: {
    onLogout: () => Promise<void>;
  };
}
