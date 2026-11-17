/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import {
  AuthenticatingScreen,
  AuthServiceNotReachableScreen,
  LoadingUserDataScreen,
  LoginFailedScreen,
  OnboardingRejectedScreen,
  SessionExpiredScreen,
} from "./AuthScreens";

/**
 * Meta Information
 *
 * Every screen an auth provider renders instead of the application. Authenticating,
 * AuthServiceNotReachable and LoginFailed belong to the Keycloak provider alone; the rest are
 * shared with `ProxyAuthProvider`.
 */

const meta = {
  title: "Auth Screens",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Authenticating: StoryObj<typeof meta> = {
  name: "Authenticating",
  render: () => <AuthenticatingScreen />,
};

export const AuthServiceNotReachable: StoryObj<typeof meta> = {
  name: "Auth service not reachable",
  render: () => <AuthServiceNotReachableScreen />,
};

export const LoginFailed: StoryObj<typeof meta> = {
  name: "Login failed",
  render: () => <LoginFailedScreen />,
};

export const LoadingUserData: StoryObj<typeof meta> = {
  name: "Loading user data",
  render: () => <LoadingUserDataScreen />,
};

export const SessionExpired: StoryObj<typeof meta> = {
  name: "Session expired after HTTP 401",
  render: () => <SessionExpiredScreen onSignInAgain={() => console.info("sign in again")} />,
};

export const OnboardingRejectedWithContact: StoryObj<typeof meta> = {
  name: "Onboarding rejected with contact address",
  render: () => <OnboardingRejectedScreen onLogout={() => console.info("logout")} logoutPending={false} contactEmail="info@neckar.it" />,
};

// Without a contactEmail signing out is the only action left
export const OnboardingRejectedWithoutContact: StoryObj<typeof meta> = {
  name: "Onboarding rejected without contact address",
  render: () => <OnboardingRejectedScreen onLogout={() => console.info("logout")} logoutPending={false} />,
};
