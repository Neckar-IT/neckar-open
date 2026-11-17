import type { ArticleConditionsLabourPriceRow } from "@neckar-hub/typescript-utils/openapi-types";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import { EuroOutput } from "../quantities";
import { ArticleConditionsLabourBreakdown } from "./ArticleConditionsLabourBreakdown";

/**
 * The conditions table cell of the labour, laid out like {@link ArticleConditionsPurchasedPosition}:
 * `8,5 h × 75,00 €/h → 637,50 €`.
 */
export const ArticleConditionsLabourPosition = entityPropertyRenderer("labour")<ArticleConditionsLabourPriceRow>({
  displayName: "ArticleConditionsLabourPosition",
  render: ({ entity }) => (
    // One inline child: a truncation zone makes the marker a flex container, whose items drop the spaces and the baseline.
    <span className="whitespace-nowrap">
      <span className="text-xs">
        <ArticleConditionsLabourBreakdown labour={entity} rateClassName="text-muted-foreground" />
      </span>
      <span className="text-muted-foreground">{" → "}</span>
      <span className="font-semibold">
        <EuroOutput value={entity.sellingPrice} />
      </span>
    </span>
  ),
});
