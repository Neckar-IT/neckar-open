/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Label } from "../label";
import { Switch as SwitchComponent } from "./Switch";

const meta = {
  title: "Primitives / Switch",
  component: SwitchComponent,
} satisfies Meta;

export default meta;

export const Switch: StoryObj = {
  render: () => {
    return (
      <div className="flex items-center space-x-2">
        <SwitchComponent id="airplane-mode" />
        <Label htmlFor="airplane-mode">Airplane Mode</Label>
      </div>
    );
  },
};
