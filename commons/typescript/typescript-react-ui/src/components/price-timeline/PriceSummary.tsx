import type { PriceTimelineEntryView, PriceTimelineSummary } from "@neckar-hub/typescript-utils/openapi-types";
import { EuroOutput } from "../quantities/money/euros";
import { TimelineSummaryInline } from "../value-timeline";

interface PriceSummaryProps {
  summary: PriceTimelineSummary | null;
  variant: "full" | "compact";
  className?: string;
}

/**
 * Display component for a server-computed price summary — the `Money` instantiation of
 * {@link TimelineSummaryInline}.
 *
 * `full` shows the current price followed by a `since <date>` marker and, if present, the next
 * upcoming price as `<price> from <date>`. `compact` shows only the current price.
 * Missing data anywhere in the chain renders as `NoValue`.
 *
 * Prices are integer cents on the wire; rendered via {@link EuroOutput}.
 */
export function PriceSummary({ summary, variant, className }: PriceSummaryProps): React.ReactElement {
  return (
    <TimelineSummaryInline
      summary={summary}
      variant={variant}
      className={className}
      renderValue={(entry: PriceTimelineEntryView) => <EuroOutput value={entry.price} />}
    />
  );
}
