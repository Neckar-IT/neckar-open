import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import type { TimelineEntryLike } from "./timeline.types";
import { ValidFromDate } from "./ValidFromDate";

/**
 * Displays the `validFrom` of any timeline entry as a date.
 */
export const TimelineEntryValidFrom = entityPropertyRenderer("timelineEntry")<TimelineEntryLike>({
  displayName: "TimelineEntryValidFrom",
  render: ({ entity }) => <ValidFromDate isoInstant={entity.validFrom} />,
});
