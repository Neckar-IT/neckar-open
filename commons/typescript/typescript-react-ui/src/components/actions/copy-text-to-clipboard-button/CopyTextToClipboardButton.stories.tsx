/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { CopyTextToClipboardButton, type CopyTextToClipboardButtonProps } from "./CopyTextToClipboardButton.js";

/**
 * Meta Information
 */

const meta = {
  title: "Actions / CopyTextToClipboardButton",
} satisfies Meta<CopyTextToClipboardButtonProps>;

export default meta;

/**
 * Stories
 */

export const CopyTextToClipboardButtonStory: StoryObj<CopyTextToClipboardButtonProps> = {
  name: "Copy Text to Clipboard Button",
  args: {
    text: "Hello, World!",
  },
  render: (props: CopyTextToClipboardButtonProps) => <CopyTextToClipboardButton {...props} />,
};
