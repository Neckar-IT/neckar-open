/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { SelectIdFromList } from "./SelectIdFromList";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / SelectIdFromList",
};

export default meta;

/**
 * Mock List
 */

interface Breed {
  id: string;
  name: string;
  origin: string;
}

const breeds: Breed[] = [
  { id: "YDoQ9gDFWV", name: "Labrador Retriever", origin: "Canada" },
  { id: "RnO9305Cx5", name: "German Shepherd", origin: "Germany" },
  { id: "prm9puC0E3", name: "Golden Retriever", origin: "Scotland" },
  { id: "Esuaoi1yZ1", name: "Bulldog", origin: "England" },
  { id: "Iuljc9HZDt", name: "Beagle", origin: "England" },
];

interface Args {
  disabled: boolean;
  isInvalid: boolean;
}

/**
 * Stories
 */

export const SelectIdFromListStory: Story<Args> = (args) => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <SelectIdFromList
        {...args}
        value={value}
        onChange={setValue}
        options={breeds}
        itemRenderer={(breed) => `${breed.name}, ${breed.origin}`}
        placeholder="Select a breed"
      />

      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
SelectIdFromListStory.storyName = "SelectIdFromList";
SelectIdFromListStory.args = {
  disabled: false,
  isInvalid: false,
};
