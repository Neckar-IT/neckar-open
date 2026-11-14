/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Fragment } from "react";
import { Separator } from "../separator";
import { ScrollArea as ScrollAreaComponent } from "./ScrollArea";

/**
 * Meta Information
 */

export default {
  title: "Primitives / ScrollArea",
} satisfies StoryDefault;

/**
 * Mock Data
 */

const tags = Array.from({ length: 50 }).map((_, i, a) => `v1.2.0-beta.${(a.length - i).toString()}`);

/**
 * Stories
 */

export const ScrollArea: Story = () => {
  return (
    <ScrollAreaComponent className="h-72 w-48 rounded-md border">
      <div className="p-4">
        <h4 className="mb-4 text-sm leading-none font-medium">Tags</h4>
        {tags.map((tag) => (
          <Fragment key={tag}>
            <div className="text-sm">{tag}</div>
            <Separator className="my-2" />
          </Fragment>
        ))}
      </div>
    </ScrollAreaComponent>
  );
};
