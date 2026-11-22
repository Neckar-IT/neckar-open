import { ValueTimelineEntryStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";

/**
 * `Future` if `validFrom > now`, `Current` if `validFrom` equals that of the summary's `currentEntry`, else `Past`.
 * The summary decides `Current` because the table holds only one page of entries.
 */
export function classifyTimelineEntry(
  entry: TimelineEntryLike,
  summary: TimelineSummaryLike<TimelineEntryLike>,
  now: Date,
): ValueTimelineEntryStatus {
  const entryMs = new Date(entry.validFrom).getTime();
  if (entryMs > now.getTime()) {
    return ValueTimelineEntryStatus.Future;
  }
  if (summary.currentEntry !== null && summary.currentEntry.validFrom === entry.validFrom) {
    return ValueTimelineEntryStatus.Current;
  }
  return ValueTimelineEntryStatus.Past;
}
