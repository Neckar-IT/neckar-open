import type { PriceTimelineSummary } from "../openapi-types";

/**
 * Extracts the current price in cents from a {@link PriceTimelineSummary} snapshot.
 *
 * Takes the summary rather than the entity holding it: the field it sits in is named after the
 * timelined property (`priceSummary`, `serviceRateSummary`), which only the caller knows.
 */
export function extractCurrentPrice(summary: PriceTimelineSummary | null): number | null {
  return summary?.currentEntry?.price ?? null;
}
