/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { TextInput } from "../text-input";

const meta = {
  title: "Inputs / InputWrapper",
} satisfies Meta;

export default meta;

export const InputWrapperStory: StoryObj<typeof meta> = {
  render: () => {
    const [value, setValue] = useState<string | null>(null);

    return (
      <div className="flex w-80 flex-col gap-8">
        <div className="flex flex-col gap-1">
          <p className="font-mono text-sm font-semibold">default:</p>
          <TextInput value={value} onChange={setValue} />
        </div>
        <div className="flex flex-col gap-1">
          <p className="font-mono text-sm font-semibold">disabled:</p>
          <TextInput value={value} onChange={setValue} disabled={true} />
        </div>
        <div className="flex flex-col gap-1">
          <p className="font-mono text-sm font-semibold">isInvalid:</p>
          <TextInput value={value} onChange={setValue} isInvalid={true} />
        </div>
      </div>
    );
  },
};
