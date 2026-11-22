import { PriceTimelineEntriesSortCriteria, ValueTimelineEntryStatus } from "@neckar-hub/typescript-utils/openapi-types";
import type { ReactNode } from "react";
import { TranslateEmbedded } from "../../localization/TranslateEmbedded";
import { ActionsDropdown } from "../actions";
import type { ResponsiveColumnDefinition } from "../data-display/responsive-table";
import { classifyTimelineEntry } from "./classifyTimelineEntry";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import { TimelineEntryBadge } from "./TimelineEntryBadge";
import { TimelineEntryValidFrom } from "./TimelineEntryValidFrom";

/**
 * The sort axis of every value timeline is `validFrom` — the criteria enum carries no value and
 * is therefore shared by all instantiations.
 */
export type TimelineSortKey = PriceTimelineEntriesSortCriteria;

/**
 * `validFrom` timestamp column — sortable via the server endpoint.
 */
export function validFromColumn<TEntry extends TimelineEntryLike>(): ResponsiveColumnDefinition<TEntry, TimelineSortKey> {
  return {
    id: "validFrom",
    header: <TranslateEmbedded textKey="timeline.fields.validFrom" />,
    sortBy: PriceTimelineEntriesSortCriteria.ValidFrom,
    cell: (entry) => <TimelineEntryValidFrom timelineEntry={entry} />,
  };
}

/**
 * Status column showing a past / current / future badge relative to `now`.
 *
 * "Current" is decided by the server via [summary.currentEntry] — the page
 * carries only a slice of entries, so the frontend cannot scan the whole list.
 */
export function statusColumn<TEntry extends TimelineEntryLike>({
  summary,
  now,
}: {
  summary: TimelineSummaryLike<TimelineEntryLike>;
  now: Date;
}): ResponsiveColumnDefinition<TEntry, TimelineSortKey> {
  return {
    id: "status",
    header: <TranslateEmbedded textKey="timeline.fields.status" />,
    fixedWidth: "144px",
    cell: (entry) => <TimelineEntryBadge status={classifyTimelineEntry(entry, summary, now)} />,
  };
}

/** Renders the row actions on future-dated rows only: past and current entries are immutable. */
export function rowActionsColumn<TEntry extends TimelineEntryLike>({
  summary,
  now,
  rowActions,
}: {
  summary: TimelineSummaryLike<TimelineEntryLike>;
  now: Date;
  /** Renders a row component, so the page's per-row action hooks run inside a component and commons never sees the parent's types. */
  rowActions: (entry: TEntry) => ReactNode;
}): ResponsiveColumnDefinition<TEntry, TimelineSortKey> {
  return {
    id: "actions",
    cellClassName: "text-right",
    fixedWidth: "72px",
    cell: (entry) => {
      const status: ValueTimelineEntryStatus = classifyTimelineEntry(entry, summary, now);
      if (status !== ValueTimelineEntryStatus.Future) return null;
      return <ActionsDropdown>{rowActions(entry)}</ActionsDropdown>;
    },
  };
}
