import type { ArticleConditionsTimelineSummary } from "./conditions-timeline.types";

/**
 * Whether an {@link ArticleConditionsTimelineSummary} snapshot carries any entry (current or upcoming).
 *
 * Returns false for a null summary or one whose currentEntry and nextFutureEntry are both null.
 */
export function hasConditionsData(summary: ArticleConditionsTimelineSummary | null): boolean {
  return summary !== null && (summary.currentEntry !== null || summary.nextFutureEntry !== null);
}
