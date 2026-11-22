/**
 * Imports
 */

import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/storybook";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { TimelineNavParent, type TimelineNavParentProps } from "./TimelineNavParent";

const meta = {
  title: "Navigation / Timeline Nav",
  component: TimelineNavParent,
  decorators: [
    (Story) => (
      <WithMemoryRouter path="/:leafId" url="/step-2">
        <Story />
      </WithMemoryRouter>
    ),
  ],
} satisfies Meta<TimelineNavParentProps>;

export default meta;

export const TimelineNavParentStory: StoryObj<TimelineNavParentProps> = {
  name: "TimelineNavParent",
  args: {
    title: "Parent",
    status: "completed",
    firstElement: true,
    lastElement: true,
    leafs: [
      { type: "leaf", title: "Child 1", status: "completed", href: "/step-1" },
      { type: "leaf", title: "Child 2", status: "completed", href: "/step-2" },
      { type: "leaf", title: "Child 3", status: "completed", href: "/step-3" },
    ],
  },
  render: (props: TimelineNavParentProps) => <TimelineNavParent {...props} />,
};
