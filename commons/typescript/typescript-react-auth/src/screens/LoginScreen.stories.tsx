/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import type { LoginResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { delay } from "es-toolkit";
import { LoginScreen } from "./LoginScreen";

/**
 * Meta Information
 *
 * The screen the CredentialsAuthProvider renders while nobody is signed in. The stories differ in
 * what their onLogin returns.
 */

const meta: StoryDefault = {
  title: "Credentials Auth / Login Screen",
};

export default meta;

/**
 * Mocks
 */

const successfulLogin = async (): Promise<LoginResponse> => {
  await delay(1000);
  return {
    type: "good",
    data: {
      jwsTokens: { accessToken: "valid-access-token", refreshToken: "valid-refresh-token" },
      loginName: "test-user",
    },
  };
};

const rejectedLogin = async (): Promise<LoginResponse> => {
  await delay(1000);
  return { type: "bad/InvalidCredentials" };
};

const disabledUserLogin = async (): Promise<LoginResponse> => {
  await delay(1000);
  return { type: "bad/UserDisabled" };
};

/**
 * Stories
 */

export const WithHeaderAndFooter: Story = () => (
  <LoginScreen onLogin={successfulLogin} header={<div>This is the Header!</div>} footer={<div>This is the Footer!</div>} />
);
WithHeaderAndFooter.storyName = "With header and footer";

export const WithoutHeaderAndFooter: Story = () => <LoginScreen onLogin={successfulLogin} />;
WithoutHeaderAndFooter.storyName = "Without header and footer";

// Submit the form to see the alert - the screen renders no error state on its own.
export const InvalidCredentials: Story = () => <LoginScreen onLogin={rejectedLogin} />;
InvalidCredentials.storyName = "Invalid credentials";

// Submit the form to see the alert - the screen renders no error state on its own.
export const UserDisabled: Story = () => <LoginScreen onLogin={disabledUserLogin} />;
UserDisabled.storyName = "User disabled";
