import type {
  ArticleConditionsLabourPriceRow,
  ArticleConditionsPriceRow,
  ArticleConditionsPriceSummary,
} from "@neckar-hub/typescript-utils/openapi-types";
import { ChevronRightIcon } from "lucide-react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { FormSectionHeader } from "../forms/form";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "../primitives/collapsible";
import { Table, TableBody, TableCaption, TableCell, TableFooter, TableHead, TableHeader, TableRow } from "../primitives/table";
import { EuroOutput } from "../quantities";
import { NoValue } from "../text/no-value";
import { ArticleConditionsLabourBreakdown } from "./ArticleConditionsLabourBreakdown";
import { ArticleConditionsRowSurcharge } from "./ArticleConditionsRowSurcharge";

interface ArticleConditionsPriceOverviewProps {
  /** `undefined` until the companion first answered. */
  summary: ArticleConditionsPriceSummary | undefined;
}

/**
 * The price overview at the end of the add-conditions form: one row per position, the customer price last.
 * Collapsed until opened.
 * Lays out server-calculated figures only — see {@link useConnectArticleConditionsPriceSummaryCompanion}.
 */
export function ArticleConditionsPriceOverview({ summary }: ArticleConditionsPriceOverviewProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <Collapsible>
      <CollapsibleTrigger className="group flex items-center gap-1.5 hover:cursor-pointer">
        <ChevronRightIcon className="h-4 w-4 transition-transform duration-200 group-data-[state=open]:rotate-90" />
        <FormSectionHeader>{t("conditionsTimeline.priceOverview.title")}</FormSectionHeader>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <ArticleConditionsPriceTable summary={summary} />
      </CollapsibleContent>
    </Collapsible>
  );
}

function ArticleConditionsPriceTable({ summary }: ArticleConditionsPriceOverviewProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  return (
    <>
      {summary === undefined ? (
        <NoValue>{t("conditionsTimeline.priceOverview.placeholder")}</NoValue>
      ) : (
        <Table>
          <TableCaption>
            {summary.unit === null
              ? t("conditionsTimeline.priceOverview.perPiece")
              : t({ key: "conditionsTimeline.priceOverview.perUnit", options: { unit: summary.unit } })}
          </TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>{t("conditionsTimeline.priceOverview.position")}</TableHead>
              <TableHead className="text-right">{t("conditionsTimeline.priceOverview.purchasePrice")}</TableHead>
              <TableHead className="text-right">{t("conditionsTimeline.priceOverview.sellingPrice")}</TableHead>
              <TableHead className="text-right">{t("conditionsTimeline.priceOverview.surcharge")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <PurchasedPositionRow label={t("conditionsTimeline.priceOverview.part")} row={summary.part} />
            {summary.accessory !== null && (
              <PurchasedPositionRow label={t("conditionsTimeline.priceOverview.accessory")} row={summary.accessory} />
            )}
            {summary.labour !== null && <LabourPositionRow labour={summary.labour} />}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell>{t("conditionsTimeline.priceOverview.customerPrice")}</TableCell>
              <TableCell />
              <TableCell className="text-right">
                <EuroOutput value={summary.customerPrice} />
              </TableCell>
              <TableCell />
            </TableRow>
          </TableFooter>
        </Table>
      )}
    </>
  );
}

function PurchasedPositionRow({ label, row }: { label: string; row: ArticleConditionsPriceRow }): React.ReactElement {
  return (
    <TableRow>
      <TableCell>{label}</TableCell>
      <TableCell className="text-right">
        <EuroOutput value={row.inputPrice} />
      </TableCell>
      <TableCell className="text-right">
        <EuroOutput value={row.sellingPrice} />
      </TableCell>
      <TableCell className="text-right">
        <ArticleConditionsRowSurcharge row={row} />
      </TableCell>
    </TableRow>
  );
}

function LabourPositionRow({ labour }: { labour: ArticleConditionsLabourPriceRow }): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  return (
    <TableRow>
      <TableCell>
        {t("conditionsTimeline.priceOverview.labour")} <ArticleConditionsLabourBreakdown labour={labour} />
      </TableCell>
      <TableCell className="text-right">
        <NoValue />
      </TableCell>
      <TableCell className="text-right">
        <EuroOutput value={labour.sellingPrice} />
      </TableCell>
      <TableCell className="text-right">
        <NoValue />
      </TableCell>
    </TableRow>
  );
}
