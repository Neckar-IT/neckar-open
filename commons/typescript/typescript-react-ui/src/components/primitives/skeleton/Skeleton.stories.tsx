/**
 * Imports
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { Skeleton as SkeletonComponent } from "./Skeleton";

/**
 * Meta Information
 */

const meta = {
  title: "Primitives / Skeleton",
} satisfies Meta;

export default meta;

/**
 * Stories
 */

export const Skeleton: StoryObj<typeof meta> = {
  render: () => {
    return (
      <div className="flex items-center space-x-4">
        <SkeletonComponent className="h-12 w-12 rounded-full" />
        <div className="space-y-2">
          <SkeletonComponent className="h-4 w-[250px]" />
          <SkeletonComponent className="h-4 w-[200px]" />
        </div>
      </div>
    );
  },
};
