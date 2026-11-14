/**
 * Imports
 */

import type { Story, StoryDefault } from "@ladle/react";
import type { Tag } from "@neckar-hub/typescript-utils/openapi-types";
import { useState } from "react";
import { Separator } from "../../primitives/separator";
import { exampleTags } from "./exampleTags";
import { TagPicker, type TagPickerProps } from "./TagPicker";

/**
 * Meta Information
 */

const meta: StoryDefault<Omit<TagPickerProps, "value" | "onChange" | "options">> = {
  title: "Inputs / TagPicker",
};

export default meta;

/**
 * Stories
 */

export const TagPickerStory: Story<Omit<TagPickerProps, "value" | "onChange" | "options">> = (props) => {
  const [value, setValue] = useState<Tag[] | null>(null);

  return (
    <>
      <TagPicker {...props} options={exampleTags} value={value} onChange={setValue} />
      <Separator className="mt-8" />
      <pre className="bg-muted mt-8 px-4 py-2 text-sm">{JSON.stringify(value, null, 2)}</pre>
    </>
  );
};
TagPickerStory.storyName = "TagPicker";
TagPickerStory.args = {
  disabled: false,
};
