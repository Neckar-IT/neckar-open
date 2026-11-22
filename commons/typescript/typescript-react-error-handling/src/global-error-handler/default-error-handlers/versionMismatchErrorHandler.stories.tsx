/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { VersionMismatchPage } from "./versionMismatchErrorHandler";

const meta = {
  title: "Error Handling / Default Error Handlers",
  component: VersionMismatchPage,
} satisfies Meta;

export default meta;

export const VersionMismatchErrorHandler: StoryObj = {
  render: () => <VersionMismatchPage />,
};
