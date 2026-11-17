/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { OptionContainer } from "./OptionContainer";

/**
 * Meta Information
 */

const meta = {
  title: "Layout / OptionContainer",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const OptionContainerStory: StoryObj<typeof meta> = {
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
