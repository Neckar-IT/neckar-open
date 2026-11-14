/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Marker, MarkerContent } from "./Marker";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Marker",
} satisfies StoryDefault;

/**
 * Stories
 */

export const MarkerSeparator: Story = () => {
  return (
    <div className="w-80 space-y-4">
      <Marker>
        <MarkerContent>Default</MarkerContent>
      </Marker>
      <Marker variant="separator">
        <MarkerContent>Separator</MarkerContent>
      </Marker>
      <Marker variant="border">
        <MarkerContent>Border</MarkerContent>
      </Marker>
    </div>
  );
};
