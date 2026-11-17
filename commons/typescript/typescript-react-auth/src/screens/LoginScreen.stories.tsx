/**
 * Imports
 */

import type { LoginResponse } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { LoginScreen } from "./LoginScreen";

/**
 * Meta Information
 *
 * The screen the CredentialsAuthProvider renders while nobody is signed in. The stories differ in
 * what their onLogin returns.
 */

const meta = {
  title: "Credentials Auth / Login Screen",
} satisfies Meta;

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

export const WithHeaderAndFooter: StoryObj<typeof meta> = {
  name: "With header and footer",
  render: () => <LoginScreen onLogin={successfulLogin} header={<div>This is the Header!</div>} footer={<div>This is the Footer!</div>} />,
};

export const WithoutHeaderAndFooter: StoryObj<typeof meta> = {
  name: "Without header and footer",
  render: () => <LoginScreen onLogin={successfulLogin} />,
};

// Submit the form to see the alert - the screen renders no error state on its own.
export const InvalidCredentials: StoryObj<typeof meta> = {
  name: "Invalid credentials",
  render: () => <LoginScreen onLogin={rejectedLogin} />,
};

// Submit the form to see the alert - the screen renders no error state on its own.
export const UserDisabled: StoryObj<typeof meta> = {
  name: "User disabled",
  render: () => <LoginScreen onLogin={disabledUserLogin} />,
};
