/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { LogoutContextProvider } from "../context/LogoutContext";
import { LogoutAndReloadScreen } from "./LogoutAndReloadScreen";

/**
 * The screen the CredentialsAuthProvider renders on `/logout`. Its onLogout never settles here -
 * otherwise the redirect would navigate the story browser away.
 */

const meta = {
  title: "Credentials Auth / Logout Screen",
  component: LogoutAndReloadScreen,
} satisfies Meta;

export default meta;

export const LoggingOut: StoryObj = {
  name: "Logging out",
  render: () => (
    <LogoutContextProvider onLogout={() => new Promise<void>(() => {})}>
      <LogoutAndReloadScreen />
    </LogoutContextProvider>
  ),
};
