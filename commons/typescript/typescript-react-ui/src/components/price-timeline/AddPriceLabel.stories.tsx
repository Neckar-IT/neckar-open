import type { Story, StoryDefault } from "@ladle/react";
import { AddPriceLabel } from "./AddPriceLabel";

export default {
  title: "Price timeline / AddPriceLabel",
} satisfies StoryDefault;

export const Default: Story = () => <AddPriceLabel />;

export const WrappedInLink: Story = () => (
  <a href="/price-timeline/new" className="underline-offset-4 hover:underline">
    <AddPriceLabel />
  </a>
);
