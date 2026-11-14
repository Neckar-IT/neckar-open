/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Label } from "../label";
import { Switch as SwitchComponent } from "./Switch";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Switch",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Switch: Story = () => {
  return (
    <div className="flex items-center space-x-2">
      <SwitchComponent id="airplane-mode" />
      <Label htmlFor="airplane-mode">Airplane Mode</Label>
    </div>
  );
};
