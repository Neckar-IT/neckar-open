import type { ArticleConditionsTimelineSummary, ArticleConditionsToDisplay } from "@neckar-hub/typescript-utils/conditions-timeline";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../data-display/code-block";
import { ArticleConditionsSummaryHeadline } from "./ArticleConditionsSummaryHeadline";
import { currentConditionsEntry, futureConditionsEntry } from "./conditionsFixtures";

const meta = {
  title: "Conditions timeline / ArticleConditionsSummaryHeadline",
  component: ArticleConditionsSummaryHeadline,
} satisfies Meta;

export default meta;

interface Case {
  label: string;
  summary: ArticleConditionsTimelineSummary<ArticleConditionsToDisplay> | null;
}

const cases: Case[] = [
  {
    label: "currentEntry + nextFutureEntry",
    summary: { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry },
  },
  { label: "currentEntry only", summary: { currentEntry: currentConditionsEntry, nextFutureEntry: null } },
  { label: "empty summary (both entries null)", summary: { currentEntry: null, nextFutureEntry: null } },
  { label: "summary null", summary: null },
];

export const ArticleConditionsSummaryHeadlineStory: StoryObj = {
  name: "ArticleConditionsSummaryHeadline",
  render: () => {
    return (
      <div className="grid max-w-xl gap-6">
        {cases.map((entry) => (
          <div key={entry.label}>
            <CodeBlock className="mb-2">{entry.label}</CodeBlock>
            <ArticleConditionsSummaryHeadline summary={entry.summary} />
          </div>
        ))}
      </div>
    );
  },
};
