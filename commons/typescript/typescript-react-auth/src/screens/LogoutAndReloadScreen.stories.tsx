/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { LogoutContextProvider } from "../context/LogoutContext";
import { LogoutAndReloadScreen } from "./LogoutAndReloadScreen";

/**
 * Meta Information
 *
 * The screen the CredentialsAuthProvider renders on `/logout`. Its onLogout never settles here -
 * otherwise the redirect would navigate the story browser away.
 */

const meta: StoryDefault = {
  title: "Credentials Auth / Logout Screen",
};

export default meta;

/**
 * Stories
 */

export const LoggingOut: Story = () => (
  <LogoutContextProvider onLogout={() => new Promise<void>(() => {})}>
    <LogoutAndReloadScreen />
  </LogoutContextProvider>
);
LoggingOut.storyName = "Logging out";
