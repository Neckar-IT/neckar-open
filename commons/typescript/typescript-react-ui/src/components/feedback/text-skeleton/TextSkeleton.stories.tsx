import type { Meta, StoryObj } from "@storybook/react-vite";
import { TextSkeleton as TextSkeletonComponent } from "./TextSkeleton";

const meta = {
  title: "Feedback / TextSkeleton",
  component: TextSkeletonComponent,
} satisfies Meta;

export default meta;

export const Default: StoryObj = {
  render: () => <TextSkeletonComponent />,
};

export const InlineWithText: StoryObj = {
  render: () => (
    <p>
      Loading title: <TextSkeletonComponent /> — rest of the sentence.
    </p>
  ),
};

export const CustomWidth: StoryObj = {
  render: () => (
    <div className="space-y-2">
      <TextSkeletonComponent className="w-12" />
      <TextSkeletonComponent className="w-24" />
      <TextSkeletonComponent className="w-48" />
      <TextSkeletonComponent className="w-96" />
    </div>
  ),
};
