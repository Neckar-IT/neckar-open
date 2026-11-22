/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { OptionContainer } from "./OptionContainer";

const meta = {
  title: "Layout / OptionContainer",
  component: OptionContainer,
} satisfies Meta;

export default meta;

export const OptionContainerStory: StoryObj = {
  name: "Option Container",
  render: () => {
    const [checked, setChecked] = useState(false);

    return (
      <div className="space-y-8">
        <div>
          <pre>default:</pre>
          <OptionContainer className="px-6 py-4" variant="default">
            Content
          </OptionContainer>
        </div>

        <div>
          <pre>button:</pre>
          <OptionContainer className="px-6 py-4" variant="button" onClick={() => window.alert("Clicked")}>
            Content
          </OptionContainer>
        </div>

        <div>
          <pre>radio:</pre>
          <OptionContainer checked={checked} className="px-6 py-4" variant="radio" onClick={() => setChecked((prev) => !prev)}>
            Content
          </OptionContainer>
        </div>
      </div>
    );
  },
};
