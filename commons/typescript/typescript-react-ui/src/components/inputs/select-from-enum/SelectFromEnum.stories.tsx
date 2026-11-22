/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { CodeBlock } from "../../data-display/code-block";
import { SelectFromEnum } from "./SelectFromEnum";

const meta = {
  title: "Inputs / SelectFromEnum",
  component: SelectFromEnum,
} satisfies Meta;

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

export const SelectFromEnumStory: StoryObj = {
  name: "SelectFromEnum",
  render: () => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <SelectFromEnum value={value} onChange={setValue} enumObject={ChickenBreed} />
        <CodeBlock>{`value: ${JSON.stringify(value, null, 2)}`}</CodeBlock>
      </div>
    );
  },
};
