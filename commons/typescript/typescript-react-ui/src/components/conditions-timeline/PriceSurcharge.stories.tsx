import type { PriceWithProfit } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../data-display/code-block";
import { PriceSurcharge } from "./PriceSurcharge";

const meta = {
  title: "Conditions timeline / PriceSurcharge",
} satisfies Meta;

export default meta;

interface Case {
  label: string;
  price: PriceWithProfit | null | undefined;
}

const cases: Case[] = [
  { label: "gain — 50,00 € purchased, 68,00 € sold", price: { inputPrice: 5000, sellingPrice: 6800 } },
  { label: "sold at cost — surcharge of exactly zero", price: { inputPrice: 5000, sellingPrice: 5000 } },
  { label: "loss — sold below the purchase price", price: { inputPrice: 5000, sellingPrice: 4000 } },
  { label: "zero purchase price — rate undefined, profit shown alone", price: { inputPrice: 0, sellingPrice: 6800 } },
  { label: "both prices zero", price: { inputPrice: 0, sellingPrice: 0 } },
  { label: "price null — explicitly absent", price: null },
  { label: "price undefined — still loading", price: undefined },
];

export const PriceSurchargeStory: StoryObj<typeof meta> = {
  name: "PriceSurcharge",
  render: () => {
    return (
      <div className="grid max-w-xl gap-6">
        {cases.map((entry) => (
          <div key={entry.label}>
            <CodeBlock className="mb-2">{entry.label}</CodeBlock>
            <PriceSurcharge price={entry.price} />
          </div>
        ))}
      </div>
    );
  },
};
