/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { FilterByTextInput } from "./FilterByTextInput";

/**
 * Meta Information
 */

const meta = {
  title: "Filters / FilterByTextInput",
  decorators: [
    (Story) => (
      <div className="mx-auto w-80">
        <Story />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const FilterByTextInputStory: StoryObj<typeof meta> = {
  name: "FilterByTextInput",
  render: () => {
    const [value, setValue] = useState<string | undefined>(undefined);

    return <FilterByTextInput value={value} onChange={setValue} />;
  },
};
