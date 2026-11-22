/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { Slider as SliderComponent } from "./Slider";

const meta = {
  title: "Primitives / Slider",
  component: SliderComponent,
  decorators: [
    (Component) => (
      <div className="max-w-screen-sm">
        <Component />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

export const Slider: StoryObj = {
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
