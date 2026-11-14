/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { Delayed as DelayedComponent } from "./Delayed";

/**
 * Meta Information
 */

export default {
  title: "Feedback / Delayed",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Delayed: Story = () => {
  return (
    <>
      <p className="mb-2">The following content will only show after 1 second:</p>
      <DelayedComponent delay={1000}>
        <div className="rounded-md border p-4">Content</div>
      </DelayedComponent>
    </>
  );
};
