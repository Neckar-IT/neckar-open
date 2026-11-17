/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Badge as BadgeComponent } from "./Badge";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Badge",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Badge: StoryObj<typeof meta> = {
  render: () => {
    return (
      <div>
        <h3>Variants</h3>
        <div className="mt-2 flex flex-col items-start space-y-4">
          <BadgeComponent variant="default">Default</BadgeComponent>
          <BadgeComponent variant="secondary">Secondary</BadgeComponent>
          <BadgeComponent variant="destructive">Destructive</BadgeComponent>
          <BadgeComponent variant="outline">Outline</BadgeComponent>
        </div>
      </div>
    );
  },
};
