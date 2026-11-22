import type { Meta, StoryObj } from "@storybook/react-vite";
import { FlatQuoteTable } from "./FlatQuoteTable";
import {
  bookOrderQuote,
  discountedBookOrderQuote,
  internalLineQuote,
  longDescriptionQuote,
  mixedVatRateQuote,
  optionalLineQuote,
} from "./quoteFixtures";

const meta = {
  title: "Quote / FlatQuoteTable",
  component: FlatQuoteTable,
} satisfies Meta;

export default meta;

export const UndiscountedStory: StoryObj = {
  render: () => <FlatQuoteTable quote={bookOrderQuote} />,
};

export const DiscountedStory: StoryObj = {
  render: () => <FlatQuoteTable quote={discountedBookOrderQuote} />,
};

export const LongDescriptionStory: StoryObj = {
  render: () => <FlatQuoteTable quote={longDescriptionQuote} />,
};

export const InternalLineStory: StoryObj = {
  render: () => <FlatQuoteTable quote={internalLineQuote} />,
};

export const OptionalLineStory: StoryObj = {
  render: () => <FlatQuoteTable quote={optionalLineQuote} />,
};

export const MixedVatRateStory: StoryObj = {
  render: () => <FlatQuoteTable quote={mixedVatRateQuote} />,
};
