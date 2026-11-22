/**
 * Imports
 */

import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { TimelineNavLeaf, type TimelineNavLeafProps } from "./TimelineNavLeaf";

const meta = {
  title: "Navigation / Timeline Nav",
  component: TimelineNavLeaf,
  decorators: [
    (Story) => {
      return (
        <WithMemoryRouter path="/:stepId" params={{ stepId: "step-1" }}>
          <Story />
        </WithMemoryRouter>
      );
    },
  ],
} satisfies Meta;

export default meta;

export const TimelineNavLeafStory: StoryObj<TimelineNavLeafProps> = {
  name: "TimelineNavLeaf",
  args: {
    title: "Step 1",
    status: "completed",
  },
  argTypes: {
    status: {
      control: { type: "select" },
      options: ["not-started", "in-progress", "completed", "warning", "error"],
    },
  },
  render: (props: TimelineNavLeafProps) => {
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
  },
};
