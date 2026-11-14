import type { UseFormReturn } from "react-hook-form";
import { ArticleConditionsPriceOverview } from "./ArticleConditionsPriceOverview";
import type { ArticleConditionsTimelineEntryUpsertRequestSchema } from "./useArticleConditionsTimelineEntryFormResolver";
import {
  type FetchArticleConditionsPriceSummary,
  useConnectArticleConditionsPriceSummaryCompanion,
} from "./useConnectArticleConditionsPriceSummaryCompanion";

interface ArticleConditionsPriceOverviewSectionProps {
  form: UseFormReturn<ArticleConditionsTimelineEntryUpsertRequestSchema>;
  /** The product's generated `calculateArticleConditionsPriceSummary`. */
  fetchPriceSummary: FetchArticleConditionsPriceSummary;
}

/**
 * The price overview bound to its form — a page mounts it as the last child of its form content.
 */
export function ArticleConditionsPriceOverviewSection({
  form,
  fetchPriceSummary,
}: ArticleConditionsPriceOverviewSectionProps): React.ReactElement {
  return <ArticleConditionsPriceOverview summary={useConnectArticleConditionsPriceSummaryCompanion(form, fetchPriceSummary)} />;
}
