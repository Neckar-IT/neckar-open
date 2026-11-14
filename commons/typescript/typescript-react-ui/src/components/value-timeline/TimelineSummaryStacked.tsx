import { cn } from "cn";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { NoValue } from "../text/no-value";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import { ValidFromDate } from "./ValidFromDate";

interface TimelineSummaryStackedProps<TEntry extends TimelineEntryLike> {
  summary: TimelineSummaryLike<TEntry> | null;
  variant: "full" | "compact";
  /**
   * Renders the entry's value with the type class of its row. The only value-specific part.
   */
  renderValue: (entry: TEntry, valueClassName: string) => React.ReactNode;
  /**
   * Type class of the value in the current-entry row (and in the `compact` variant). The upcoming
   * entry always renders one step smaller, muted, in its own row.
   */
  currentValueClassName?: string;
  className?: string;
}

/**
 * Display of a server-computed timeline summary for values that do not fit on one line — a
 * structured value such as `ArticleConditions` (part price, accessory, installation time) rather
 * than a single amount.
 *
 * `full` puts the current value on its own row — undated, because "what applies right now" needs no
 * start date — and the upcoming value on a second, muted row led by its "from <date>", the date that
 * does carry information. Keeping the rows apart stops a long value from wrapping into the date of
 * the upcoming entry — which is what {@link TimelineSummaryInline} does when it is handed more than
 * a short scalar. `compact` shows only the current value.
 *
 * Missing data anywhere in the chain renders as {@link NoValue}.
 */
export function TimelineSummaryStacked<TEntry extends TimelineEntryLike>({
  summary,
  variant,
  renderValue,
  currentValueClassName = "text-base",
  className,
}: TimelineSummaryStackedProps<TEntry>): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  const currentEntry = summary?.currentEntry ?? null;
  const nextFutureEntry = summary?.nextFutureEntry ?? null;

  if (variant === "compact") {
    if (currentEntry === null) {
      return <NoValue className={className} />;
    }
    return <span className={className}>{renderValue(currentEntry, currentValueClassName)}</span>;
  }

  if (currentEntry === null && nextFutureEntry === null) {
    return <NoValue className={className} />;
  }

  return (
    <div className={cn("flex min-w-0 flex-col gap-0.5", className)}>
      {currentEntry === null ? <NoValue className={currentValueClassName} /> : renderValue(currentEntry, currentValueClassName)}

      {nextFutureEntry !== null ? (
        <div className="text-muted-foreground flex min-w-0 flex-wrap items-baseline gap-x-2 text-xs">
          <span>
            {t("timeline.from")} <ValidFromDate isoInstant={nextFutureEntry.validFrom} />:
          </span>
          {renderValue(nextFutureEntry, "text-xs")}
        </div>
      ) : null}
    </div>
  );
}
