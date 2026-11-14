/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Badge as BadgeComponent } from "./Badge";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Badge",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Badge: Story = () => {
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
};
