/**
 * Imports
 */

import type { RegisterResponse } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { delay } from "es-toolkit";
import { RegistrationScreen } from "./RegistrationScreen";

/**
 * The self-signup screen, rendered by the CredentialsAuthProvider on `/register`. The stories
 * differ in what their onRegister returns.
 */

const meta = {
  title: "Credentials Auth / Registration Screen",
  component: RegistrationScreen,
} satisfies Meta;

export default meta;

/**
 * Mocks
 */

const successfulRegistration = async (): Promise<RegisterResponse> => {
  await delay(500);
  return {
    type: "good",
    data: {
      jwsTokens: {
        accessToken: "access-token",
        refreshToken: "refresh-token",
      },
    },
  };
};

const rejectedRegistration = async (): Promise<RegisterResponse> => {
  await delay(500);
  return { type: "bad/AlreadyRegistered" };
};

export const EmptyForm: StoryObj = {
  name: "Empty form",
  render: () => <RegistrationScreen onRegister={successfulRegistration} />,
};

export const WithHeader: StoryObj = {
  name: "With header",
  render: () => <RegistrationScreen onRegister={successfulRegistration} header={<div>This is the Header!</div>} />,
};

// Submit the form to see the alert - the screen renders no error state on its own.
export const EmailAlreadyRegistered: StoryObj = {
  name: "Email already registered",
  render: () => <RegistrationScreen onRegister={rejectedRegistration} />,
};
