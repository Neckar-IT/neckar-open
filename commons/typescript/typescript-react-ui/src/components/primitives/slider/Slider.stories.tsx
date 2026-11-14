/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { Slider as SliderComponent } from "./Slider";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Slider",
  decorators: [
    (Component) => (
      <div className="max-w-screen-sm">
        <Component />
      </div>
    ),
  ],
} satisfies StoryDefault;

/**
 * Stories
 */

export const Slider: Story = () => {
  const [value, setValue] = useState(50);

  return (
    <>
      <SliderComponent value={value} onValueChange={setValue} />
      <div className="mt-4">
        <span>current value: {value}</span>
      </div>
    </>
  );
};
