import { ValueTimelineEntryStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { Badge } from "../primitives/badge";

interface TimelineEntryBadgeProps {
  status: ValueTimelineEntryStatus;
}

const variantByStatus: Record<ValueTimelineEntryStatus, "secondary" | "default" | "outline"> = {
  [ValueTimelineEntryStatus.Past]: "secondary",
  [ValueTimelineEntryStatus.Current]: "default",
  [ValueTimelineEntryStatus.Future]: "outline",
};

export function TimelineEntryBadge({ status }: TimelineEntryBadgeProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return <Badge variant={variantByStatus[status]}>{t(`enums.ValueTimelineEntryStatus.${status}`)}</Badge>;
}
