import { type Story, type StoryDefault } from "@ladle/react";
import { TextSkeleton as TextSkeletonComponent } from "./TextSkeleton";

export default {
  title: "Feedback / TextSkeleton",
} satisfies StoryDefault;

export const Default: Story = () => <TextSkeletonComponent />;

export const InlineWithText: Story = () => (
  <p>
    Loading title: <TextSkeletonComponent /> — rest of the sentence.
  </p>
);

export const CustomWidth: Story = () => (
  <div className="space-y-2">
    <TextSkeletonComponent className="w-12" />
    <TextSkeletonComponent className="w-24" />
    <TextSkeletonComponent className="w-48" />
    <TextSkeletonComponent className="w-96" />
  </div>
);
