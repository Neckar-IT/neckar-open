import type { ArticleConditionsTimelineTableEntry } from "@neckar-hub/typescript-utils/conditions-timeline";
import type {
  ArticleConditions,
  ArticleConditionsWithAccessory,
  ArticleConditionsWithInstallationTime,
  FullArticleConditions,
} from "@neckar-hub/typescript-utils/openapi-types";
import { TranslateEmbedded } from "../../localization/TranslateEmbedded";
import { ColumnHeaderWithHint } from "../data-display/column-header-with-hint";
import type { ResponsiveColumnDefinition } from "../data-display/responsive-table";
import { EuroOutput } from "../quantities";
import type { TimelineSortKey } from "../value-timeline";
import { ArticleConditionsLabourPosition } from "./ArticleConditionsLabourPosition";
import { ArticleConditionsPurchasedPosition } from "./ArticleConditionsPurchasedPosition";

/**
 * A column of a conditions timeline, over the {@link ArticleConditions} implementation its entries carry. A
 * column that shows the part fits every article type; one that shows an optional part asks for a shape
 * declaring it, so a table of an article type without that part cannot carry the column.
 */
type ConditionsColumn<Conditions extends ArticleConditions = ArticleConditions> = ResponsiveColumnDefinition<
  ArticleConditionsTimelineTableEntry<Conditions>,
  TimelineSortKey
>;

/** Fits `8.888,88 € +424 % → 8.888,88 €`, four-digit amounts at their widest digits, without clipping. */
const PurchasedPositionColumnWidth = "232px";

/** Fits `88,5 h × 188,88 €/h → 8.888,88 €`, four-digit amounts at their widest digits, without clipping. */
const LabourPositionColumnWidth = "240px";

export function partColumn(): ConditionsColumn {
  return {
    id: "part",
    header: (
      <ColumnHeaderWithHint hint={<TranslateEmbedded textKey="conditionsTimeline.table.partHint" />}>
        <TranslateEmbedded textKey="conditionsTimeline.priceOverview.part" />
      </ColumnHeaderWithHint>
    ),
    headerClassName: "justify-end",
    cellClassName: "text-right",
    fixedWidth: PurchasedPositionColumnWidth,
    cell: (entry) => <ArticleConditionsPurchasedPosition position={entry.priceSummary.part} unit={entry.priceSummary.unit} />,
  };
}

/**
 * Empty for an article of an accessory-carrying type that is supplied without one.
 */
export function accessoryColumn(): ConditionsColumn<ArticleConditionsWithAccessory> {
  return {
    id: "accessory",
    header: (
      <ColumnHeaderWithHint hint={<TranslateEmbedded textKey="conditionsTimeline.table.accessoryHint" />}>
        <TranslateEmbedded textKey="conditionsTimeline.priceOverview.accessory" />
      </ColumnHeaderWithHint>
    ),
    headerClassName: "justify-end",
    cellClassName: "text-right",
    fixedWidth: PurchasedPositionColumnWidth,
    cell: (entry) => <ArticleConditionsPurchasedPosition position={entry.priceSummary.accessory} unit={entry.priceSummary.unit} />,
  };
}

/**
 * Empty for an article of a mounted type that carries no installation time of its own.
 */
export function labourColumn(): ConditionsColumn<ArticleConditionsWithInstallationTime> {
  return {
    id: "labour",
    header: (
      <ColumnHeaderWithHint hint={<TranslateEmbedded textKey="conditionsTimeline.table.labourHint" />}>
        <TranslateEmbedded textKey="conditionsTimeline.priceOverview.labour" />
      </ColumnHeaderWithHint>
    ),
    headerClassName: "justify-end",
    cellClassName: "text-right",
    fixedWidth: LabourPositionColumnWidth,
    cell: (entry) => <ArticleConditionsLabourPosition labour={entry.priceSummary.labour} />,
  };
}

/**
 * Left out by {@link articleConditionsColumns}: without an optional part it repeats the part's selling price.
 */
export function customerPriceColumn(): ConditionsColumn {
  return {
    id: "customerPrice",
    header: <TranslateEmbedded textKey="conditionsTimeline.priceOverview.customerPrice" />,
    headerClassName: "justify-end",
    cellClassName: "text-right",
    cell: (entry) => <EuroOutput value={entry.priceSummary.customerPrice} />,
  };
}

export function articleConditionsColumns(): ConditionsColumn[] {
  return [partColumn()];
}

export function articleConditionsWithAccessoryColumns(): ConditionsColumn<ArticleConditionsWithAccessory>[] {
  return [partColumn(), accessoryColumn(), customerPriceColumn()];
}

export function articleConditionsWithInstallationTimeColumns(): ConditionsColumn<ArticleConditionsWithInstallationTime>[] {
  return [partColumn(), labourColumn(), customerPriceColumn()];
}

export function fullArticleConditionsColumns(): ConditionsColumn<FullArticleConditions>[] {
  return [partColumn(), accessoryColumn(), labourColumn(), customerPriceColumn()];
}
