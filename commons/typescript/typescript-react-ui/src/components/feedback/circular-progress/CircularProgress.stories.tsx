/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { CircularProgress as CircularProgressComponent, type Props } from "./CircularProgress";

/**
 * Meta Information
 */

const meta = {
  title: "Feedback / CircularProgress",
  decorators: [
    (Component) => (
      <div className="flex items-center justify-center">
        <Component />
      </div>
    ),
  ],
  argTypes: {
    value: {
      control: {
        type: "range",
        min: 0,
        max: 100,
        step: 1,
      },
    },
  },
} satisfies Meta<Props>;

export default meta;

/**
 * Stories
 */

export const CircularProgress: StoryObj<Props> = {
  args: {
    value: 50,
    className: "h-20 w-20",
  },
  render: (args: Props) => {
    return <CircularProgressComponent {...args} />;
  },
};
