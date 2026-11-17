import type { PriceTimelineEntryView, PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { EuroOutput } from "../quantities/money/euros";
import { TimelineSummaryStats } from "../value-timeline";

interface PriceSummaryHeadlineProps {
  summary: PriceTimelineSummary | null;
  className?: string;
}

/**
 * Summary for a price timeline: the current price and — when a future entry is scheduled — the next
 * change, rendered as two same-sized stats. The `Money` instantiation of
 * {@link TimelineSummaryStats}.
 *
 * Card-less by design: it sits in the table toolbar's `filters` slot, opposite the add-entry action,
 * so the summary and the primary action share one row.
 */
export function PriceSummaryHeadline({ summary, className }: PriceSummaryHeadlineProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <TimelineSummaryStats
      summary={summary}
      currentLabel={t("priceSummary.current")}
      nextChangeLabel={t("priceSummary.nextChange")}
      className={className}
      renderValue={(entry: PriceTimelineEntryView, valueClassName: string) => <EuroOutput value={entry.price} className={valueClassName} />}
    />
  );
}
