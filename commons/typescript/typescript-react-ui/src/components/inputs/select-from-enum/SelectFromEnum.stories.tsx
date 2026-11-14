/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { SelectFromEnum } from "./SelectFromEnum";

/**
 * Meta Information
 */

const meta: StoryDefault = {
  title: "Inputs / SelectFromEnum",
};

export default meta;

/**
 * Mock Enum
 */

const ChickenBreed = {
  Silkie: "Silkie",
  RhodeIslandRed: "RhodeIslandRed",
  AyamCemani: "AyamCemani",
  Leghorn: "Leghorn",
  Orpington: "Orpington",
} as const;

/**
 * Stories
 */

export const SelectFromEnumStory: Story = () => {
  const [value, setValue] = useState<string | null>(null);

  return (
    <div className="flex w-80 flex-col gap-8">
      <SelectFromEnum value={value} onChange={setValue} enumObject={ChickenBreed} />
      <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
    </div>
  );
};
SelectFromEnumStory.storyName = "SelectFromEnum";
