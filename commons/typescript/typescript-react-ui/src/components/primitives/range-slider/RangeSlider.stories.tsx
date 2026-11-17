/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState } from "react";
import { RangeSlider as RangeSliderComponent } from "./RangeSlider";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / RangeSlider",
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

export const RangeSlider: StoryObj<typeof meta> = {
  render: () => {
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
  },
};
