import type { PriceTimelineSummary } from "../openapi-types";

/**
 * Whether a {@link PriceTimelineSummary} snapshot carries any price entry (current or upcoming).
 *
 * Returns false for a null summary or one whose currentEntry and nextFutureEntry are both null.
 */
export function hasPriceData(summary: PriceTimelineSummary | null): boolean {
  return summary !== null && (summary.currentEntry !== null || summary.nextFutureEntry !== null);
}
