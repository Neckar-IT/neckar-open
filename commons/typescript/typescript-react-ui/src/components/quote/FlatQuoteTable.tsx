import {
  type FlatQuoteDetails,
  type FlatQuoteLineDetails,
  FlatQuoteLineDetailsItemWithTypeType,
  Visibility,
} from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { CirclePlusIcon, EyeOffIcon } from "lucide-react";
import { Fragment, useMemo } from "react";
import { useEmbeddedTranslation } from "../../localization/useEmbeddedTranslation";
import { ResponsiveTable, type ResponsiveTableConfig } from "../data-display/responsive-table";
import { TableFrame } from "../layout/table-frame";
import { IconWithTooltip } from "../media/icon-with-tooltip";
import { TooltipProvider } from "../primitives/tooltip";
import { EuroOutput, VatRateOutput } from "../quantities";
import { AmountOutput } from "./AmountOutput";
import { QuoteSumsBlock } from "./QuoteSumsBlock";

interface FlatQuoteTableProps {
  quote: FlatQuoteDetails;
}

/**
 * A quote as the rows of a bill: one row per section and per item, numbered by its place in the tree,
 * the sum block below the table.
 */
export function FlatQuoteTable({ quote }: FlatQuoteTableProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();

  const config = useMemo<ResponsiveTableConfig<FlatQuoteLineDetails, never>>(
    () => ({
      rowClassName: (row) => cn({ "text-muted-foreground": row.visibility === Visibility.Internal }),
      columns: [
        {
          id: "position",
          header: t("quote.fields.position"),
          viewOnMobile: "secondary-header",
          relativeWidth: 1,
          cell: (row) => <span style={{ paddingLeft: `${indentRem(row.depth)}rem` }}>{row.position}</span>,
        },
        {
          id: "headline",
          header: t("quote.fields.description"),
          viewOnMobile: "primary-header",
          relativeWidth: 4,
          cellClassName: "whitespace-normal break-words",
          cell: (row) => (
            <>
              <div className="flex items-center gap-1.5">
                {row.visibility === Visibility.Internal ? (
                  <IconWithTooltip tooltip={t("quote.internalLine")}>
                    <EyeOffIcon aria-label={t("quote.internalLine")} />
                  </IconWithTooltip>
                ) : null}
                <span className="font-medium">{row.headline}</span>
                {row.optionality === "Optional" ? (
                  <IconWithTooltip tooltip={t("quote.optional.tooltip")}>
                    <CirclePlusIcon aria-label={t("quote.optional.tooltip")} />
                  </IconWithTooltip>
                ) : null}
              </div>
              {row.details === null ? null : <div className="text-muted-foreground text-xs">{row.details}</div>}
            </>
          ),
        },
        {
          id: "vat",
          header: t("quote.fields.vat"),
          headerClassName: "justify-end",
          cellClassName: "text-right",
          relativeWidth: 1,
          cell: (row) =>
            row.vatRates.map((vatRate, index) => (
              <Fragment key={vatRate}>
                {index === 0 ? null : " / "}
                <VatRateOutput value={vatRate} />
              </Fragment>
            )),
        },
        {
          id: "amount",
          header: t("quote.fields.amount"),
          headerClassName: "justify-end",
          cellClassName: "text-right",
          relativeWidth: 1,
          cell: (row) => (row.amount === null ? null : <AmountOutput amount={row.amount} />),
        },
        {
          id: "unitPrice",
          header: t("quote.fields.unitPrice"),
          headerClassName: "justify-end",
          cellClassName: "text-right",
          relativeWidth: 1.5,
          cell: (row) => (row.type === FlatQuoteLineDetailsItemWithTypeType.Item ? <EuroOutput value={row.unitPrice} /> : null),
        },
        {
          id: "lineTotal",
          header: t("quote.fields.lineTotal"),
          headerClassName: "justify-end",
          cellClassName: "text-right",
          relativeWidth: 1.5,
          cell: (row) => <EuroOutput value={row.total} />,
        },
      ],
    }),
    [t],
  );

  return (
    <TooltipProvider>
      <TableFrame>
        <ResponsiveTable<FlatQuoteLineDetails, never> data={quote.lines} config={config} />
        <div className="border-t-4 border-double p-4">
          <QuoteSumsBlock sums={quote.sums} />
        </div>
      </TableFrame>
    </TooltipProvider>
  );
}

/** Capped: past the cap the indent would push the number out of its column, which clips. */
function indentRem(depth: number): number {
  return Math.min(depth - 1, MaxIndentLevels) * 0.75;
}

const MaxIndentLevels = 4;
