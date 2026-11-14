/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Empty as EmptyComponent } from "./Empty";

/**
 * Meta Information
 */

export default {
  title: "Feedback / Empty",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Empty: Story = () => <EmptyComponent className="w-32" />;
