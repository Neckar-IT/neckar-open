/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Empty as EmptyComponent } from "./Empty";

/**
 * Meta Information
 */

const meta = {
  title: "Feedback / Empty",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Empty: StoryObj<typeof meta> = {
  render: () => <EmptyComponent className="w-32" />,
};
