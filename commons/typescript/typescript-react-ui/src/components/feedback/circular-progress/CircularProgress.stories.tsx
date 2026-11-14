/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { CircularProgress as CircularProgressComponent, type Props } from "./CircularProgress";

/**
 * Meta Information
 */

export default {
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
} satisfies StoryDefault<Props>;

/**
 * Stories
 */

export const CircularProgress: Story<Props> = (args) => {
  return <CircularProgressComponent {...args} />;
};

CircularProgress.args = {
  value: 50,
  className: "h-20 w-20",
};
