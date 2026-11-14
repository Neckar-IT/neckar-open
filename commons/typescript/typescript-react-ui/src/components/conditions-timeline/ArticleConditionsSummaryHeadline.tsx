import type {
  ArticleConditionsTimelineEntry,
  ArticleConditionsTimelineSummary,
  ArticleConditionsToDisplay,
} from "@neckar-hub/typescript-utils/conditions-timeline";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { TimelineSummaryStats } from "../value-timeline";
import { ArticleConditionsLine } from "./ArticleConditionsLine";

interface ArticleConditionsSummaryHeadlineProps {
  summary: ArticleConditionsTimelineSummary<ArticleConditionsToDisplay> | null;
  className?: string;
}

/**
 * Summary for a conditions timeline: the current conditions and — when a future entry is scheduled —
 * the next change, rendered as two same-sized stats. The {@link ArticleConditions} instantiation of
 * {@link TimelineSummaryStats}.
 *
 * Card-less by design: it sits in the table toolbar's `filters` slot, opposite the add-entry action,
 * so the summary and the primary action share one row.
 */
export function ArticleConditionsSummaryHeadline({ summary, className }: ArticleConditionsSummaryHeadlineProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <TimelineSummaryStats
      summary={summary}
      currentLabel={t("conditionsSummary.current")}
      nextChangeLabel={t("conditionsSummary.nextChange")}
      className={className}
      valueClassName="text-lg"
      renderValue={(entry: ArticleConditionsTimelineEntry<ArticleConditionsToDisplay>, valueClassName: string) => (
        <ArticleConditionsLine articleConditions={entry.conditions} priceClassName={valueClassName} />
      )}
    />
  );
}
