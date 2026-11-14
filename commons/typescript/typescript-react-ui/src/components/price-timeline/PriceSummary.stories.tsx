import type { Story, StoryDefault } from "@ladle/react";
import type { PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { CodeBlock } from "../data-display/code-block";
import { PriceSummary } from "./PriceSummary";

export default {
  title: "Price timeline / PriceSummary",
} satisfies StoryDefault;

const currentEntry = { price: 1499, validFrom: "2020-01-01T00:00:00Z" };
const futureEntry = { price: 1800, validFrom: "2099-06-01T00:00:00Z" };

const variants = ["full", "compact"] as const;

interface Case {
  label: string;
  summary: PriceTimelineSummary | null;
}

const cases: Case[] = [
  { label: "currentEntry only", summary: { currentEntry, nextFutureEntry: null } },
  { label: "currentEntry + nextFutureEntry", summary: { currentEntry, nextFutureEntry: futureEntry } },
  { label: "nextFutureEntry only (no currentEntry)", summary: { currentEntry: null, nextFutureEntry: futureEntry } },
  { label: "empty summary (both entries null)", summary: { currentEntry: null, nextFutureEntry: null } },
  { label: "summary null", summary: null },
];

export const PriceSummaryStory: Story = () => {
  return (
    <div className="grid grid-cols-2 gap-4">
      {cases.flatMap((entry) =>
        variants.map((variant) => (
          <div key={`${entry.label}-${variant}`}>
            <CodeBlock className="mb-4">{`${entry.label} — ${variant}`}</CodeBlock>
            <PriceSummary summary={entry.summary} variant={variant} />
          </div>
        )),
      )}
    </div>
  );
};
PriceSummaryStory.storyName = "PriceSummary";
