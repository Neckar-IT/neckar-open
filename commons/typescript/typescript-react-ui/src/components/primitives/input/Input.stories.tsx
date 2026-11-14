/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Input as InputComponent } from "./Input";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Input",
  decorators: [
    (Story) => (
      <div className="w-80">
        <Story />
      </div>
    ),
  ],
} satisfies StoryDefault;

/**
 * Stories
 */

export const Input: Story = () => {
  return <InputComponent placeholder="Email" type="email" />;
};
