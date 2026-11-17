/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Label } from "../label";
import { Switch as SwitchComponent } from "./Switch";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Switch",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Switch: StoryObj<typeof meta> = {
  render: () => {
    return (
      <div className="flex items-center space-x-2">
        <SwitchComponent id="airplane-mode" />
        <Label htmlFor="airplane-mode">Airplane Mode</Label>
      </div>
    );
  },
};
