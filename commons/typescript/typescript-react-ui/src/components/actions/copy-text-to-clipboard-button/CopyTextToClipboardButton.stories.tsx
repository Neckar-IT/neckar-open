/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { CopyTextToClipboardButton, type CopyTextToClipboardButtonProps } from "./CopyTextToClipboardButton.js";

const meta = {
  title: "Actions / CopyTextToClipboardButton",
  component: CopyTextToClipboardButton,
} satisfies Meta<CopyTextToClipboardButtonProps>;

export default meta;

export const CopyTextToClipboardButtonStory: StoryObj<CopyTextToClipboardButtonProps> = {
  name: "Copy Text to Clipboard Button",
  args: {
    text: "Hello, World!",
  },
  render: (props: CopyTextToClipboardButtonProps) => <CopyTextToClipboardButton {...props} />,
};
