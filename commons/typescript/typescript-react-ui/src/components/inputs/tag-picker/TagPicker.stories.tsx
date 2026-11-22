/**
 * Imports
 */

import type { Tag } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Separator } from "../../primitives/separator";
import { exampleTags } from "./exampleTags";
import { TagPicker, type TagPickerProps } from "./TagPicker";

const meta = {
  title: "Inputs / TagPicker",
  component: TagPicker,
} satisfies Meta<typeof TagPicker>;

export default meta;

export const TagPickerStory: StoryObj<Omit<TagPickerProps, "value" | "onChange" | "options">> = {
  name: "TagPicker",
  args: {
    disabled: false,
  },
  render: (props: Omit<TagPickerProps, "value" | "onChange" | "options">) => {
    const [value, setValue] = useState<Tag[] | null>(null);

    return (
      <>
        <TagPicker {...props} options={exampleTags} value={value} onChange={setValue} />
        <Separator className="mt-8" />
        <pre className="bg-muted mt-8 px-4 py-2 text-sm">{JSON.stringify(value, null, 2)}</pre>
      </>
    );
  },
};
