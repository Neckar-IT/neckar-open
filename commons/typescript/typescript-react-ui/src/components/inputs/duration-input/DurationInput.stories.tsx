/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { DurationInput } from "./DurationInput";

/**
 * Meta Information
 */

export default {
  title: "Inputs / DurationInput",
} satisfies StoryDefault;

/**
 * Stories
 */

export const DurationInputStory: Story = () => {
  const [value, setValue] = useState<number>(0);

  return (
    <div className="flex space-x-8">
      <div className="w-80">
        <h3 className="mb-2 font-bold">Default:</h3>
        <DurationInput max={90000} step={5000} value={value} onChange={setValue} />
        <pre className="bg-muted mt-8 rounded-md px-4 py-2">{`value: ${value.toString()} ms`}</pre>
      </div>
      <div className="w-80">
        <h3 className="mb-2 font-bold">Disabled:</h3>
        <DurationInput disabled value={75000} onChange={setValue} />
      </div>
    </div>
  );
};
DurationInputStory.storyName = "DurationInput";
