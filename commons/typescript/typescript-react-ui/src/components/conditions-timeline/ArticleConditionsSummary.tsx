import type {
  ArticleConditionsTimelineEntry,
  ArticleConditionsTimelineSummary,
  ArticleConditionsToDisplay,
} from "@neckar-hub/typescript-utils/conditions-timeline";
import { TimelineSummaryStacked } from "../value-timeline";
import { ArticleConditionsLine } from "./ArticleConditionsLine";

interface ArticleConditionsSummaryProps {
  summary: ArticleConditionsTimelineSummary<ArticleConditionsToDisplay> | null;
  variant: "full" | "compact";
  className?: string;
}

/**
 * Display component for a server-computed conditions summary — the {@link ArticleConditions}
 * instantiation of {@link TimelineSummaryStacked}.
 *
 * `full` puts the current conditions on their own row, undated, and — if present — the upcoming
 * conditions on a second, muted row led by `from <date>`. A conditions record is several values
 * wide, so the two entries need separate rows to stay readable. `compact` shows only the current
 * conditions. Missing data anywhere in the chain renders as `NoValue`.
 */
export function ArticleConditionsSummary({ summary, variant, className }: ArticleConditionsSummaryProps): React.ReactElement {
  return (
    <TimelineSummaryStacked
      summary={summary}
      variant={variant}
      className={className}
      currentValueClassName="text-sm"
      renderValue={(entry: ArticleConditionsTimelineEntry<ArticleConditionsToDisplay>, valueClassName: string) => (
        <ArticleConditionsLine articleConditions={entry.conditions} priceClassName={valueClassName} />
      )}
    />
  );
}
