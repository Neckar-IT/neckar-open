import type { Story, StoryDefault } from "@ladle/react";
import type { PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { CodeBlock } from "../data-display/code-block";
import { PriceSummaryHeadline } from "./PriceSummaryHeadline";

export default {
  title: "Price timeline / PriceSummaryHeadline",
} satisfies StoryDefault;

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

export const PriceSummaryHeadlineStory: Story = () => {
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
};
PriceSummaryHeadlineStory.storyName = "PriceSummaryHeadline";
