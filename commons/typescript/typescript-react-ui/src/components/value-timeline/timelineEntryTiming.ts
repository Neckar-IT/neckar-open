import type { TimelineEntryTimingSchema } from "./timelineEntryTimingSchema";

/** The timing of an upsert request, the `Now` | `ScheduledFor` union of the REST schema. */
export type TimelineEntryTiming = { type: "now" } | { type: "scheduled"; validFrom: string };

/** Throws for a scheduled entry without `validFrom`, a form `useTimelineEntryFormResolver` rejects before submit. */
export function timelineEntryTiming(values: TimelineEntryTimingSchema): TimelineEntryTiming {
  if (values.type === "now") {
    return { type: "now" };
  }
  if (values.validFrom == null) {
    throw new Error("useTimelineEntryFormResolver rejects a scheduled entry without validFrom");
  }
  return { type: "scheduled", validFrom: values.validFrom.toISOString() };
}
