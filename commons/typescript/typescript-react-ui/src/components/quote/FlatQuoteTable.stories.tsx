import type { Story, StoryDefault } from "@ladle/react";
import { FlatQuoteTable } from "./FlatQuoteTable";
import {
  bookOrderQuote,
  discountedBookOrderQuote,
  internalLineQuote,
  longDescriptionQuote,
  mixedVatRateQuote,
  optionalLineQuote,
} from "./quoteFixtures";

export default {
  title: "Quote / FlatQuoteTable",
} satisfies StoryDefault;

export const UndiscountedStory: Story = () => <FlatQuoteTable quote={bookOrderQuote} />;

export const DiscountedStory: Story = () => <FlatQuoteTable quote={discountedBookOrderQuote} />;

export const LongDescriptionStory: Story = () => <FlatQuoteTable quote={longDescriptionQuote} />;

export const InternalLineStory: Story = () => <FlatQuoteTable quote={internalLineQuote} />;

export const OptionalLineStory: Story = () => <FlatQuoteTable quote={optionalLineQuote} />;

export const MixedVatRateStory: Story = () => <FlatQuoteTable quote={mixedVatRateQuote} />;
