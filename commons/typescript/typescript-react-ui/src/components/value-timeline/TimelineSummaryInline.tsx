import { cn } from "cn";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { NoValue } from "../text/no-value";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import { ValidFromDate } from "./ValidFromDate";

interface TimelineSummaryInlineProps<TEntry extends TimelineEntryLike> {
  summary: TimelineSummaryLike<TEntry> | null;
  variant: "full" | "compact";
  /**
   * Renders the entry's value. The only value-specific part — a euro amount for a price
   * timeline, a condensed conditions line for a conditions timeline.
   */
  renderValue: (entry: TEntry) => React.ReactNode;
  className?: string;
}

/**
 * Inline display of a server-computed timeline summary.
 *
 * `full` shows the current value followed by a "since <date>" marker and, if present, the next
 * upcoming value as "<value> from <date>". `compact` shows only the current value.
 * Missing data anywhere in the chain renders as {@link NoValue}.
 */
export function TimelineSummaryInline<TEntry extends TimelineEntryLike>({
  summary,
  variant,
  renderValue,
  className,
}: TimelineSummaryInlineProps<TEntry>): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  if (summary === null) {
    return <NoValue className={className} />;
  }

  const { currentEntry, nextFutureEntry } = summary;

  if (variant === "compact") {
    if (currentEntry === null) {
      return <NoValue className={className} />;
    }
    return <span className={className}>{renderValue(currentEntry)}</span>;
  }

  if (currentEntry === null && nextFutureEntry === null) {
    return <NoValue className={className} />;
  }

  return (
    <span className={cn("flex min-w-0 flex-wrap items-baseline gap-x-2", className)}>
      {currentEntry === null ? <NoValue /> : renderValue(currentEntry)}

      {currentEntry !== null ? (
        <span className="text-muted-foreground text-xs">
          {t("timeline.since")} <ValidFromDate isoInstant={currentEntry.validFrom} />
        </span>
      ) : null}

      {nextFutureEntry !== null ? (
        <span className="text-muted-foreground text-xs">
          · {renderValue(nextFutureEntry)} {t("timeline.from")} <ValidFromDate isoInstant={nextFutureEntry.validFrom} />
        </span>
      ) : null}
    </span>
  );
}
