import { TranslateEmbedded } from "../../../localization/TranslateEmbedded";
import { toast } from "./toast";

/**
 * The `usedBy` an archive answer carries: one count per type of record that depends on the archived one.
 */
export interface ActiveUsageCounts {
  counts: readonly { activeCount: number }[];
}

/**
 * Shows the `actionFeedback.activeUsage` warning toast when active records still depend on the record just archived,
 * directly or through another record; shows nothing when no active record does.
 */
export function warnAboutActiveUsage(usedBy: ActiveUsageCounts): void {
  const activeCount = usedBy.counts.reduce((sum, count) => sum + count.activeCount, 0);
  if (activeCount > 0) {
    toast.warning(<TranslateEmbedded textKey={{ key: "actionFeedback.activeUsage", options: { count: activeCount } }} />);
  }
}
