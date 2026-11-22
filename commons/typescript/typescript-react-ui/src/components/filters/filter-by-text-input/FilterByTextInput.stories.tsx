/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { FilterByTextInput } from "./FilterByTextInput";

const meta = {
  title: "Filters / FilterByTextInput",
  component: FilterByTextInput,
  decorators: [
    (Story) => (
      <div className="mx-auto w-80">
        <Story />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

export const FilterByTextInputStory: StoryObj = {
  name: "FilterByTextInput",
  render: () => {
    const [value, setValue] = useState<string | undefined>(undefined);

    return <FilterByTextInput value={value} onChange={setValue} />;
  },
};
