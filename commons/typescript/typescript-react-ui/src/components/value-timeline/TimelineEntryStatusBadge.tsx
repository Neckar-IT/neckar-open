import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { Badge } from "../primitives/badge";
import type { TimelineEntryStatus } from "./classifyTimelineEntry";

interface TimelineEntryStatusBadgeProps {
  status: TimelineEntryStatus;
}

/**
 * Displays the validity status of a timeline entry as a Badge.
 *
 * Variants:
 *   - past    → secondary (muted)
 *   - current → default (primary, emphasized)
 *   - future  → outline (neutral, upcoming)
 */
const variantByStatus: Record<TimelineEntryStatus, "secondary" | "default" | "outline"> = {
  past: "secondary",
  current: "default",
  future: "outline",
};

export function TimelineEntryStatusBadge({ status }: TimelineEntryStatusBadgeProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <Badge variant={variantByStatus[status]}>{t(`timeline.status.${status}`)}</Badge>;
}
