/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
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

const meta: StoryDefault = {
  title: "Auth Screens",
};

export default meta;

/**
 * Stories
 */

export const Authenticating: Story = () => <AuthenticatingScreen />;
Authenticating.storyName = "Authenticating";

export const AuthServiceNotReachable: Story = () => <AuthServiceNotReachableScreen />;
AuthServiceNotReachable.storyName = "Auth service not reachable";

export const LoginFailed: Story = () => <LoginFailedScreen />;
LoginFailed.storyName = "Login failed";

export const LoadingUserData: Story = () => <LoadingUserDataScreen />;
LoadingUserData.storyName = "Loading user data";

export const SessionExpired: Story = () => <SessionExpiredScreen onSignInAgain={() => console.info("sign in again")} />;
SessionExpired.storyName = "Session expired after HTTP 401";

export const OnboardingRejectedWithContact: Story = () => (
  <OnboardingRejectedScreen onLogout={() => console.info("logout")} logoutPending={false} contactEmail="info@neckar.it" />
);
OnboardingRejectedWithContact.storyName = "Onboarding rejected with contact address";

// Without a contactEmail signing out is the only action left
export const OnboardingRejectedWithoutContact: Story = () => (
  <OnboardingRejectedScreen onLogout={() => console.info("logout")} logoutPending={false} />
);
OnboardingRejectedWithoutContact.storyName = "Onboarding rejected without contact address";
