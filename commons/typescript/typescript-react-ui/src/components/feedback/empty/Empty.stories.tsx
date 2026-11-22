/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Empty as EmptyComponent } from "./Empty";

const meta = {
  title: "Feedback / Empty",
  component: EmptyComponent,
} satisfies Meta;

export default meta;

export const Empty: StoryObj = {
  render: () => <EmptyComponent className="w-32" />,
};
