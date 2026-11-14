import { cn } from "cn";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { NoValue } from "../text/no-value";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import { ValidFromDate } from "./ValidFromDate";

/**
 * `leading` renders the value at full contrast (the current value); `muted` greys it out so the
 * next scheduled change stays visually subordinate while keeping the same size and baseline.
 */
export type TimelineStatEmphasis = "leading" | "muted";

export const timelineStatClassByEmphasis: Record<TimelineStatEmphasis, string> = {
  leading: "cn-font-heading leading-none font-semibold tracking-tight",
  muted: "cn-font-heading text-muted-foreground leading-none font-semibold tracking-tight",
};

interface TimelineSummaryStatsProps<TEntry extends TimelineEntryLike> {
  summary: TimelineSummaryLike<TEntry> | null;
  /** Label of the leading stat, e.g. "Aktueller Preis" / "Aktuelle Konditionen". */
  currentLabel: string;
  /** Label of the muted stat, e.g. "Nächste Änderung". */
  nextChangeLabel: string;
  /**
   * Renders the entry's value with the emphasis class of its stat. The only value-specific part.
   */
  renderValue: (entry: TEntry, className: string) => React.ReactNode;
  /**
   * Type class of both stat values. A single amount carries the default headline size; a value
   * that spans several fields needs a smaller one to stay on its line.
   */
  valueClassName?: string;
  className?: string;
}

/**
 * Summary for a value timeline: the current value and — when a future entry is scheduled — the next
 * change, rendered as two same-sized stats separated by a vertical divider. Same size keeps their
 * labels and values on one line; the next change is muted so the current value stays visually leading.
 *
 * Card-less by design: it sits in the table toolbar's `filters` slot, opposite the add-entry action,
 * so the summary and the primary action share one row. When no change is scheduled, only the
 * current-value stat renders. Missing data anywhere in the chain renders as {@link NoValue}.
 */
export function TimelineSummaryStats<TEntry extends TimelineEntryLike>({
  summary,
  currentLabel,
  nextChangeLabel,
  renderValue,
  valueClassName = "text-2xl",
  className,
}: TimelineSummaryStatsProps<TEntry>): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  const currentEntry = summary?.currentEntry ?? null;
  const nextFutureEntry = summary?.nextFutureEntry ?? null;

  return (
    <div className={cn("flex min-w-0 flex-col gap-4 sm:flex-row sm:items-start sm:gap-6", className)}>
      <TimelineStat
        emphasis="leading"
        label={currentLabel}
        entry={currentEntry}
        datePreposition={t("timeline.since")}
        renderValue={renderValue}
        valueClassName={valueClassName}
      />

      {nextFutureEntry !== null ? (
        <>
          <div className="bg-border hidden w-px self-stretch sm:block" aria-hidden />
          <TimelineStat
            emphasis="muted"
            label={nextChangeLabel}
            entry={nextFutureEntry}
            datePreposition={t("timeline.from")}
            renderValue={renderValue}
            valueClassName={valueClassName}
          />
        </>
      ) : null}
    </div>
  );
}

interface TimelineStatProps<TEntry extends TimelineEntryLike> {
  emphasis: TimelineStatEmphasis;
  label: string;
  entry: TEntry | null;
  datePreposition: string;
  renderValue: (entry: TEntry, className: string) => React.ReactNode;
  valueClassName: string;
}

function TimelineStat<TEntry extends TimelineEntryLike>({
  emphasis,
  label,
  entry,
  datePreposition,
  renderValue,
  valueClassName,
}: TimelineStatProps<TEntry>): React.ReactElement {
  return (
    <div className="flex min-w-0 flex-col gap-1">
      <span className="text-muted-foreground text-xs font-medium tracking-wide uppercase">{label}</span>
      {entry !== null ? (
        <div className="flex flex-wrap items-baseline gap-x-2">
          {renderValue(entry, cn(valueClassName, timelineStatClassByEmphasis[emphasis]))}
          <span className="text-muted-foreground text-sm">
            {datePreposition} <ValidFromDate isoInstant={entry.validFrom} />
          </span>
        </div>
      ) : (
        <NoValue className={cn(valueClassName, "leading-none")} />
      )}
    </div>
  );
}
