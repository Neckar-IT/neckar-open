/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { useState } from "react";
import { RangeSlider as RangeSliderComponent } from "./RangeSlider";

/**
 * Meta Information
 */

export default {
  title: "Primitives / RangeSlider",
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

export const RangeSlider: Story = () => {
  const [value, setValue] = useState([25, 75]);

  return (
    <>
      <RangeSliderComponent minStepsBetweenThumbs={1} value={value} onValueChange={setValue} />
      <div className="mt-4">
        <p>first value: {value[0]}</p>
        <p>second value: {value[1]}</p>
      </div>
    </>
  );
};
