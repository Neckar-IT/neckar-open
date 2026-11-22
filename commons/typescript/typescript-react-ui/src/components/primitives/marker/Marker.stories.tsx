/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Marker, MarkerContent } from "./Marker";

const meta = {
  title: "Primitives / Marker",
  component: Marker,
} satisfies Meta;

export default meta;

export const MarkerSeparator: StoryObj = {
  render: () => {
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
  },
};
