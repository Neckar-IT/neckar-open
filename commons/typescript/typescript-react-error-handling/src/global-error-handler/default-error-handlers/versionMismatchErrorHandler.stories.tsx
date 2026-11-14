/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { VersionMismatchPage } from "./versionMismatchErrorHandler";

/**
 * Meta Information
 */

export default {
  title: "Error Handling / Default Error Handlers",
} satisfies StoryDefault;

/**
 * Stories
 */

export const VersionMismatchErrorHandler: Story = () => <VersionMismatchPage />;
