import type { PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../data-display/code-block";
import { PriceSummaryHeadline } from "./PriceSummaryHeadline";

const meta = {
  title: "Price timeline / PriceSummaryHeadline",
  component: PriceSummaryHeadline,
} satisfies Meta;

export default meta;

const currentEntry = { price: 1499, validFrom: "2026-01-15T08:24:00Z" };
const futureEntry = { price: 1800, validFrom: "2099-06-01T00:00:00Z" };

interface Case {
  label: string;
  summary: PriceTimelineSummary | null;
}

const cases: Case[] = [
  { label: "currentEntry + nextFutureEntry", summary: { currentEntry, nextFutureEntry: futureEntry } },
  { label: "currentEntry only", summary: { currentEntry, nextFutureEntry: null } },
  { label: "empty summary (both entries null)", summary: { currentEntry: null, nextFutureEntry: null } },
  { label: "summary null", summary: null },
];

export const PriceSummaryHeadlineStory: StoryObj = {
  name: "PriceSummaryHeadline",
  render: () => {
    return (
      <div className="grid max-w-xl gap-6">
        {cases.map((entry) => (
          <div key={entry.label}>
            <CodeBlock className="mb-2">{entry.label}</CodeBlock>
            <PriceSummaryHeadline summary={entry.summary} />
          </div>
        ))}
      </div>
    );
  },
};
