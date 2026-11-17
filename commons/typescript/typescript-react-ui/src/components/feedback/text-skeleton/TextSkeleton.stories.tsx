import type { Meta, StoryObj } from "@storybook/react-vite";
import { TextSkeleton as TextSkeletonComponent } from "./TextSkeleton";

const meta = {
  title: "Feedback / TextSkeleton",
} satisfies Meta;

export default meta;

export const Default: StoryObj<typeof meta> = {
  render: () => <TextSkeletonComponent />,
};

export const InlineWithText: StoryObj<typeof meta> = {
  render: () => (
    <p>
      Loading title: <TextSkeletonComponent /> — rest of the sentence.
    </p>
  ),
};

export const CustomWidth: StoryObj<typeof meta> = {
  render: () => (
    <div className="space-y-2">
      <TextSkeletonComponent className="w-12" />
      <TextSkeletonComponent className="w-24" />
      <TextSkeletonComponent className="w-48" />
      <TextSkeletonComponent className="w-96" />
    </div>
  ),
};
