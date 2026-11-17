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
} satisfies Meta;

export default meta;

export const UndiscountedStory: StoryObj<typeof meta> = {
  render: () => <FlatQuoteTable quote={bookOrderQuote} />,
};

export const DiscountedStory: StoryObj<typeof meta> = {
  render: () => <FlatQuoteTable quote={discountedBookOrderQuote} />,
};

export const LongDescriptionStory: StoryObj<typeof meta> = {
  render: () => <FlatQuoteTable quote={longDescriptionQuote} />,
};

export const InternalLineStory: StoryObj<typeof meta> = {
  render: () => <FlatQuoteTable quote={internalLineQuote} />,
};

export const OptionalLineStory: StoryObj<typeof meta> = {
  render: () => <FlatQuoteTable quote={optionalLineQuote} />,
};

export const MixedVatRateStory: StoryObj<typeof meta> = {
  render: () => <FlatQuoteTable quote={mixedVatRateQuote} />,
};
