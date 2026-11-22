import type { Meta, StoryObj } from "@storybook/react-vite";
import { AddPriceLabel } from "./AddPriceLabel";

const meta = {
  title: "Price timeline / AddPriceLabel",
  component: AddPriceLabel,
} satisfies Meta;

export default meta;

export const Default: StoryObj = {
  render: () => <AddPriceLabel />,
};

export const WrappedInLink: StoryObj = {
  render: () => (
    <a href="/price-timeline/new" className="underline-offset-4 hover:underline">
      <AddPriceLabel />
    </a>
  ),
};
