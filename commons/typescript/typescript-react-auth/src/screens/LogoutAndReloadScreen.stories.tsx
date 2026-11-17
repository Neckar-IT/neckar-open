/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { LogoutContextProvider } from "../context/LogoutContext";
import { LogoutAndReloadScreen } from "./LogoutAndReloadScreen";

/**
 * Meta Information
 *
 * The screen the CredentialsAuthProvider renders on `/logout`. Its onLogout never settles here -
 * otherwise the redirect would navigate the story browser away.
 */

const meta = {
  title: "Credentials Auth / Logout Screen",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const LoggingOut: StoryObj<typeof meta> = {
  name: "Logging out",
  render: () => (
    <LogoutContextProvider onLogout={() => new Promise<void>(() => {})}>
      <LogoutAndReloadScreen />
    </LogoutContextProvider>
  ),
};
