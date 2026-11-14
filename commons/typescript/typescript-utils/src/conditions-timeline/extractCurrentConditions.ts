import type { ArticleConditionsTimelineSummary, ArticleConditionsToDisplay } from "./conditions-timeline.types";

/**
 * Extracts the currently valid conditions from an {@link ArticleConditionsTimelineSummary} snapshot.
 * Hands them out as the widest shape, so a caller can display any optional part the entity happens
 * to have.
 *
 * Takes the summary rather than the entity holding it: the field it sits in is named after the
 * timelined property (`conditionsSummary`, `purchaseTermsSummary`), which only the caller knows.
 */
export function extractCurrentConditions(
  summary: ArticleConditionsTimelineSummary<ArticleConditionsToDisplay> | null,
): ArticleConditionsToDisplay | null {
  return summary?.currentEntry?.conditions ?? null;
}
