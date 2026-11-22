/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Compass } from "./Compass";

const meta = { title: "Compass", component: Compass } satisfies Meta;

export default meta;

export const Minimal: StoryObj<{ currentValue: number }> = {
  args: {
    currentValue: 15,
  },
  render: ({ currentValue }: { currentValue: number }) => <Compass configuration={{ data: { currentValue } }} />,
};
