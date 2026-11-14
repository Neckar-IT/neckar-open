/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { WithMemoryRouter } from "@neckar-hub/typescript-react-utils/ladle";
import { TimelineNavParent, type TimelineNavParentProps } from "./TimelineNavParent";

/**
 * Meta Information
 */

const meta: StoryDefault<TimelineNavParentProps> = {
  title: "Navigation / Timeline Nav",
  decorators: [
    (Story) => (
      <WithMemoryRouter path="/:leafId" url="/step-2">
        <Story />
      </WithMemoryRouter>
    ),
  ],
};

export default meta;

/**
 * Stories
 */

export const TimelineNavParentStory: Story<TimelineNavParentProps> = (props) => <TimelineNavParent {...props} />;
TimelineNavParentStory.storyName = "TimelineNavParent";
TimelineNavParentStory.args = {
  title: "Parent",
  status: "completed",
  firstElement: true,
  lastElement: true,
  leafs: [
    { type: "leaf", title: "Child 1", status: "completed", href: "/step-1" },
    { type: "leaf", title: "Child 2", status: "completed", href: "/step-2" },
    { type: "leaf", title: "Child 3", status: "completed", href: "/step-3" },
  ],
};
