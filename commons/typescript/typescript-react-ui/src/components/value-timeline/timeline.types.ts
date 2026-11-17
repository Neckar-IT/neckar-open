/**
 * The part of a timeline entry every value timeline shares: the instant from which the entry's
 * value is in effect.
 *
 * The concrete entry views (`PriceTimelineEntryView`, `ArticleConditionsTimelineEntryView`, …)
 * add their value on top; everything in this folder works against this shape so status,
 * ordering, pagination and the timing form exist exactly once.
 */
export interface TimelineEntryLike {
  validFrom: string;
}

/**
 * The server-computed summary of a value timeline: the entry currently in effect and the next
 * scheduled one. Mirrors `PriceTimelineSummary` / `ArticleConditionsTimelineSummary`.
 */
export interface TimelineSummaryLike<TEntry extends TimelineEntryLike> {
  currentEntry: TEntry | null;
  nextFutureEntry: TEntry | null;
}
