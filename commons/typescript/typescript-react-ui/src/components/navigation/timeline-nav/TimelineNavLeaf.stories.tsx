/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import { TimelineNavLeaf, type TimelineNavLeafProps } from "./TimelineNavLeaf";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Navigation / Timeline Nav",
  decorators: [
    (Story) => {
      return (
        <WithMemoryRouter path="/:stepId" params={{ stepId: "step-1" }}>
          <Story />
        </WithMemoryRouter>
      );
    },
  ],
};

export default meta;

/**
 * Stories
 */

export const TimelineNavLeafStory: Story<TimelineNavLeafProps> = (props) => {
  return (
    <div className="space-y-8">
      <div className="flex items-center">
        <span className="text-muted-foreground w-28 font-mono text-sm">default:</span>
        <TimelineNavLeaf {...props} href="/step-0" />
      </div>
      <div className="flex items-center">
        <span className="text-muted-foreground w-28 font-mono text-sm">active:</span>
        <TimelineNavLeaf {...props} href="/step-1" />
      </div>
      <div className="flex items-center">
        <span className="text-muted-foreground w-28 font-mono text-sm">collapsed:</span>
        <TimelineNavLeaf {...props} collapsed={true} href="/step-2" />
      </div>
    </div>
  );
};
TimelineNavLeafStory.storyName = "TimelineNavLeaf";
TimelineNavLeafStory.args = {
  title: "Step 1",
  status: "completed",
};
TimelineNavLeafStory.argTypes = {
  status: {
    control: { type: "select" },
    options: ["not-started", "in-progress", "completed", "warning", "error"],
  },
};
