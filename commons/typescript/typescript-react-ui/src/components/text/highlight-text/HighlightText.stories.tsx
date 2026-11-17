/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Input } from "../../primitives/input";
import { HighlightText } from "./HighlightText";

/**
 * Meta Information
 */

const meta = {
  title: "Text / HighlightText",
} satisfies Meta;

export default meta;

/**
 * Args
 */

interface Args {
  text: string;
}

/**
 * Stories
 */

export const HighlightTextStory: StoryObj<Args> = {
  name: "Highlight Text",
  args: {
    text: "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus. Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt utlabore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.",
  },
  render: ({ text }: Args) => {
    const [search, setSearch] = useState("lorem");

    return (
      <>
        <Input
          className="w-80"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
          }}
        />
        <HighlightText text={text} className="mt-4 block" search={search} fallback="No Text provided." />
      </>
    );
  },
};
