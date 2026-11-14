/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import type { RegisterResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { delay } from "es-toolkit";
import { RegistrationScreen } from "./RegistrationScreen";

/**
 * Meta Information
 *
 * The self-signup screen, rendered by the CredentialsAuthProvider on `/register`. The stories
 * differ in what their onRegister returns.
 */

const meta: StoryDefault = {
  title: "Credentials Auth / Registration Screen",
};

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

/**
 * Stories
 */

export const EmptyForm: Story = () => <RegistrationScreen onRegister={successfulRegistration} />;
EmptyForm.storyName = "Empty form";

export const WithHeader: Story = () => <RegistrationScreen onRegister={successfulRegistration} header={<div>This is the Header!</div>} />;
WithHeader.storyName = "With header";

// Submit the form to see the alert - the screen renders no error state on its own.
export const EmailAlreadyRegistered: Story = () => <RegistrationScreen onRegister={rejectedRegistration} />;
EmailAlreadyRegistered.storyName = "Email already registered";
