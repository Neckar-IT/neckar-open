/**
 * Imports
 */

import { type Story, type StoryDefault } from "@ladle/react";
import { Skeleton as SkeletonComponent } from "./Skeleton";

/**
 * Meta Information
 */

export default {
  title: "Primitives / Skeleton",
} satisfies StoryDefault;

/**
 * Stories
 */

export const Skeleton: Story = () => {
  return (
    <div className="flex items-center space-x-4">
      <SkeletonComponent className="h-12 w-12 rounded-full" />
      <div className="space-y-2">
        <SkeletonComponent className="h-4 w-[250px]" />
        <SkeletonComponent className="h-4 w-[200px]" />
      </div>
    </div>
  );
};
