import type { ArticleConditionsPriceRow } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import { EuroOutput } from "../quantities";
import { SurchargeRateOutput } from "./PriceSurcharge";

/**
 * The conditions table cell of the part or the accessory: `182,00 € +40 % → 254,80 €`. The rate stands right of the
 * purchase price it is measured against, so it needs no label. The figures never wrap, a non-piece `unit` may move to
 * a second line: the column is as wide as four-digit figures, not as the longest unit.
 */
export const ArticleConditionsPurchasedPosition = entityPropertyRenderer("position")<ArticleConditionsPriceRow, { unit?: string | null }>({
  displayName: "ArticleConditionsPurchasedPosition",
  // Flex in every context, because a truncation zone makes the marker flex anyway; the unit is the item that may wrap.
  baseClassName: "inline-flex flex-wrap items-baseline justify-end gap-x-1",
  render: ({ entity, unit }) => (
    <>
      <span className="whitespace-nowrap">
        <span className="text-xs">
          <EuroOutput value={entity.inputPrice} />
          {entity.surchargeRate !== null ? (
            <>
              {" "}
              <span className={cn(entity.surchargeRate < 0 ? "text-destructive" : "text-muted-foreground")}>
                <SurchargeRateOutput surchargeRate={entity.surchargeRate} fractionDigits={0} />
              </span>
            </>
          ) : null}
        </span>
        <span className="text-muted-foreground">{" → "}</span>
        <span className="font-semibold">
          <EuroOutput value={entity.sellingPrice} />
        </span>
      </span>
      {unit != null ? <span className="whitespace-nowrap">/ {unit}</span> : null}
    </>
  ),
});
