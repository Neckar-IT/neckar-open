import type { Story, StoryDefault } from "@ladle/react";
import type { ArticleConditionsTimelineSummary, ArticleConditionsToDisplay } from "@neckar-hub/typescript-utils/conditions-timeline";
import { CodeBlock } from "../data-display/code-block";
import { ArticleConditionsSummaryHeadline } from "./ArticleConditionsSummaryHeadline";
import { currentConditionsEntry, futureConditionsEntry } from "./conditionsFixtures";

export default {
  title: "Conditions timeline / ArticleConditionsSummaryHeadline",
} satisfies StoryDefault;

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

export const ArticleConditionsSummaryHeadlineStory: Story = () => {
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
};
ArticleConditionsSummaryHeadlineStory.storyName = "ArticleConditionsSummaryHeadline";
