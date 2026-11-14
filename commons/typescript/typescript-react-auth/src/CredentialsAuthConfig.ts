/**
 * Imports
 */

import type { TextKey } from "@neckar-hub/typescript-react-i18n";
import type {
  LoginRequest,
  LoginResponse,
  MeResponse,
  RegisterRequest,
  RegisterResponse,
} from "@neckar-hub/typescript-utils/openapi-types";
import type { LoginScreenTextKeys } from "./screens/LoginScreen";
import type { RegistrationScreenTextKeys } from "./screens/RegistrationScreen";

/**
 * Auto Login
 */

interface AutoLoginConfiguration {
  // Which deployments log in automatically is the product's decision, not this library's.
  enabled: boolean;
  loginName: string;
  password: string;
}

/**
 * UserDetails Registration
 */

interface UserRegistrationEnabled {
  enabled: true;
  onRegister: (data: RegisterRequest) => Promise<RegisterResponse>;
  // Called after a successful registration, before the screen navigates away. Awaited, so a
  // product can flush analytics (e.g. an umami event) before the full-page reload aborts it.
  onRegisterSuccess?: () => void | Promise<void>;
  textKeys?: RegistrationScreenTextKeys;
}

interface UserRegistrationDisabled {
  enabled: false;
}

type UserRegistrationConfiguration = UserRegistrationEnabled | UserRegistrationDisabled;

/**
 * Define the Configuration for the CredentialsAuthProvider
 */

export interface CredentialsAuthConfig {
  authScreenHeader?: React.ReactNode;
  authScreenFooter?: React.ReactNode;
  onGetMe: () => Promise<MeResponse>;
  textKeys?: {
    authenticating?: TextKey;
    loggingOut?: TextKey;
  };
  login: {
    onLogin: (loginRequest: LoginRequest) => Promise<LoginResponse>;
    textKeys?: LoginScreenTextKeys;
  };
  logout?: {
    onLogout: () => Promise<void>;
  };
  autoLogin?: AutoLoginConfiguration;
  userRegistration?: UserRegistrationConfiguration;
}
