/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { FilterByTextInput } from "./FilterByTextInput";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Filters / FilterByTextInput",
  decorators: [
    (Story) => (
      <div className="mx-auto w-80">
        <Story />
      </div>
    ),
  ],
};

export default meta;

/**
 * Stories
 */

export const FilterByTextInputStory: Story = () => {
  const [value, setValue] = useState<string | undefined>(undefined);

  return <FilterByTextInput value={value} onChange={setValue} />;
};
FilterByTextInputStory.storyName = "FilterByTextInput";
