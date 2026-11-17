/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Delayed as DelayedComponent } from "./Delayed";

/**
 * Meta Information
 */

const meta = {
  title: "Feedback / Delayed",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Delayed: StoryObj<typeof meta> = {
  render: () => {
    return (
      <>
        <p className="mb-2">The following content will only show after 1 second:</p>
        <DelayedComponent delay={1000}>
          <div className="rounded-md border p-4">Content</div>
        </DelayedComponent>
      </>
    );
  },
};
