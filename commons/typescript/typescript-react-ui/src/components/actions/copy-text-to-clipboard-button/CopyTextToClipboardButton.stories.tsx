/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { CopyTextToClipboardButton, type CopyTextToClipboardButtonProps } from "./CopyTextToClipboardButton.js";

/**
 * Meta Information
 */

export default {
  title: "Actions / CopyTextToClipboardButton",
} satisfies StoryDefault<CopyTextToClipboardButtonProps>;

/**
 * Stories
 */

export const CopyTextToClipboardButtonStory: Story<CopyTextToClipboardButtonProps> = (props) => <CopyTextToClipboardButton {...props} />;
CopyTextToClipboardButtonStory.storyName = "Copy Text to Clipboard Button";
CopyTextToClipboardButtonStory.args = {
  text: "Hello, World!",
};
