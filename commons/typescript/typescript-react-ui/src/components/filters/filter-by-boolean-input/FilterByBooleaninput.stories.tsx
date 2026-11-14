import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { FilterByBooleanInput } from "./FilterByBooleanInput";

const meta: StoryDefault = {
  title: "Filters / Filter By Boolean Input",
};

export default meta;

export const FilterByBooleanInputStory: Story = () => {
  const [value, setValue] = useState<boolean>(false);

  return (
    <div className="flex flex-col gap-8">
      <FilterByBooleanInput value={value} onChange={setValue} label="Nur hängende" />
      <FilterByBooleanInput form="Panel" value={value} onChange={setValue} label="Nur hängende" />
      <pre className="bg-muted rounded-sm px-4 py-2">{`value: ${String(value)}`}</pre>
    </div>
  );
};
FilterByBooleanInputStory.storyName = "Filter By Boolean Input";
