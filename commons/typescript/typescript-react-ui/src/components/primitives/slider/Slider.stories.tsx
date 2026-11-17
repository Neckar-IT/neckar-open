/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Slider as SliderComponent } from "./Slider";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Slider",
  decorators: [
    (Component) => (
      <div className="max-w-screen-sm">
        <Component />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Slider: StoryObj<typeof meta> = {
  render: () => {
    const [value, setValue] = useState(50);

    return (
      <>
        <SliderComponent value={value} onValueChange={setValue} />
        <div className="mt-4">
          <span>current value: {value}</span>
        </div>
      </>
    );
  },
};
