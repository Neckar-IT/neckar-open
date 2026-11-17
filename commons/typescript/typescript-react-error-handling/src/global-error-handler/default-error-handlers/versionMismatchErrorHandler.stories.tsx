/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { VersionMismatchPage } from "./versionMismatchErrorHandler";

/**
 * Meta Information
 */

const meta = {
  title: "Error Handling / Default Error Handlers",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const VersionMismatchErrorHandler: StoryObj<typeof meta> = {
  render: () => <VersionMismatchPage />,
};
