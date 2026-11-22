import type {
  ArticleConditionsTimelineEntry,
  ArticleConditionsTimelineSummary,
  ArticleConditionsToDisplay,
} from "@neckar-hub/typescript-utils/conditions-timeline";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { CodeBlock } from "../data-display/code-block";
import { ArticleConditionsSummary } from "./ArticleConditionsSummary";
import {
  accessoryOnlyConditions,
  conditionsWithAccessory,
  currentConditionsEntry,
  futureConditionsEntry,
  installationTimeOnlyConditions,
  materialOnlyConditions,
  wideConditions,
} from "./conditionsFixtures";

const meta = {
  title: "Conditions timeline / ArticleConditionsSummary",
  component: ArticleConditionsSummary,
} satisfies Meta;

export default meta;

const variants = ["full", "compact"] as const;

function asCurrentEntry(conditions: ArticleConditionsToDisplay): ArticleConditionsTimelineEntry<ArticleConditionsToDisplay> {
  return { conditions, validFrom: "2025-03-17T00:00:00Z" };
}

function asUpcomingEntry(conditions: ArticleConditionsToDisplay): ArticleConditionsTimelineEntry<ArticleConditionsToDisplay> {
  return { conditions, validFrom: "2099-06-01T00:00:00Z" };
}

interface Case {
  label: string;
  summary: ArticleConditionsTimelineSummary<ArticleConditionsToDisplay> | null;
}

const cases: Case[] = [
  { label: "currentEntry only", summary: { currentEntry: currentConditionsEntry, nextFutureEntry: null } },
  {
    label: "currentEntry + nextFutureEntry",
    summary: { currentEntry: currentConditionsEntry, nextFutureEntry: futureConditionsEntry },
  },
  {
    label: "nextFutureEntry only (no currentEntry)",
    summary: { currentEntry: null, nextFutureEntry: futureConditionsEntry },
  },
  {
    label: "material only (no accessory, no installation time)",
    summary: { currentEntry: asCurrentEntry(materialOnlyConditions), nextFutureEntry: null },
  },
  {
    label: "accessory, no installation time",
    summary: { currentEntry: asCurrentEntry(accessoryOnlyConditions), nextFutureEntry: null },
  },
  {
    label: "installation time, no accessory",
    summary: { currentEntry: asCurrentEntry(installationTimeOnlyConditions), nextFutureEntry: null },
  },
  {
    label: "accessory + installation time",
    summary: { currentEntry: asCurrentEntry(conditionsWithAccessory), nextFutureEntry: null },
  },
  {
    label: "material only now, everything set from the change onwards",
    summary: {
      currentEntry: asCurrentEntry(materialOnlyConditions),
      nextFutureEntry: asUpcomingEntry(conditionsWithAccessory),
    },
  },
  {
    label: "wide values (long unit, five-digit price)",
    summary: { currentEntry: asCurrentEntry(wideConditions), nextFutureEntry: asUpcomingEntry(wideConditions) },
  },
  { label: "empty summary (both entries null)", summary: { currentEntry: null, nextFutureEntry: null } },
  { label: "summary null", summary: null },
];

export const ArticleConditionsSummaryStory: StoryObj = {
  name: "ArticleConditionsSummary",
  render: () => {
    return (
      <div className="grid grid-cols-2 gap-4">
        {cases.flatMap((entry) =>
          variants.map((variant) => (
            <div key={`${entry.label}-${variant}`}>
              <CodeBlock className="mb-4">{`${entry.label} — ${variant}`}</CodeBlock>
              <ArticleConditionsSummary summary={entry.summary} variant={variant} />
            </div>
          )),
        )}
      </div>
    );
  },
};
