import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { FilterByBooleanInput } from "./FilterByBooleanInput";

const meta = {
  title: "Filters / Filter By Boolean Input",
  component: FilterByBooleanInput,
} satisfies Meta;

export default meta;

export const FilterByBooleanInputStory: StoryObj = {
  name: "Filter By Boolean Input",
  render: () => {
    const [value, setValue] = useState<boolean>(false);

    return (
      <div className="flex flex-col gap-8">
        <FilterByBooleanInput value={value} onChange={setValue} label="Nur hängende" />
        <FilterByBooleanInput form="Panel" value={value} onChange={setValue} label="Nur hängende" />
        <pre className="bg-muted rounded-sm px-4 py-2">{`value: ${String(value)}`}</pre>
      </div>
    );
  },
};
