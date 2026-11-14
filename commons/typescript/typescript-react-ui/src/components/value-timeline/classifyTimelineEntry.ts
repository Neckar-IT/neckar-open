import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";

/**
 * Classification of an entry on the timeline relative to `now`.
 *
 * See value-timeline pattern docs for the underlying semantics.
 */
export type TimelineEntryStatus = "past" | "current" | "future";

/**
 * Classifies a single entry as past / current / future.
 *
 * - `future` if `validFrom > now`
 * - `current` if this entry is the summary's `currentEntry`
 * - `past` otherwise
 *
 * The overview table paginates entries server-side, so the frontend can no
 * longer scan the full entry list to decide which entry is current. The
 * server-computed [summary] carries that marker — we just compare `validFrom`
 * against it.
 */
export function classifyTimelineEntry(
  entry: TimelineEntryLike,
  summary: TimelineSummaryLike<TimelineEntryLike>,
  now: Date,
): TimelineEntryStatus {
  const entryMs = new Date(entry.validFrom).getTime();
  if (entryMs > now.getTime()) {
    return "future";
  }
  if (summary.currentEntry !== null && summary.currentEntry.validFrom === entry.validFrom) {
    return "current";
  }
  return "past";
}
