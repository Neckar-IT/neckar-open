import type { PriceTimelineEntryView } from "@neckar-hub/typescript-utils/openapi-types";
import { TranslateEmbedded } from "../../localization/TranslateEmbedded";
import type { ResponsiveColumnDefinition } from "../data-display/responsive-table";
import type { TimelineSortKey } from "../value-timeline";
import { PriceTimelineEntryPrice } from "./PriceTimelineEntryPrice";

/**
 * Price column — the only column of a price timeline that carries the value. Status, `validFrom`
 * and the row actions are the same for every value timeline and come from the generic
 * `value-timeline` columns.
 */
export function priceColumn(): ResponsiveColumnDefinition<PriceTimelineEntryView, TimelineSortKey> {
  return {
    id: "price",
    header: <TranslateEmbedded textKey="priceTimeline.fields.price" />,
    headerClassName: "justify-end",
    cellClassName: "text-right",
    cell: (entry) => <PriceTimelineEntryPrice priceTimelineEntry={entry} />,
  };
}
