import type { ArticleConditions } from "@neckar-hub/typescript-utils/openapi-types";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import { EuroOutput } from "../quantities/money/euros";

/**
 * Displays the selling price of the article's part — "<price> / <unit>" when the record carries an
 * explicit unit (e.g. `2,50 € / Meter`), and just "<price>" for the per-piece default (`unit ==
 * null`), where a unit suffix would add no information.
 *
 * The selling price is what a quote is built from. Prices are stored in cents on the wire.
 */
export const ArticleConditionsSellingPrice = entityPropertyRenderer("articleConditions")<ArticleConditions>({
  displayName: "ArticleConditionsSellingPrice",
  render: ({ entity }) => (
    <>
      <EuroOutput value={entity.part.sellingPrice} />
      {entity.unit != null ? ` / ${entity.unit}` : null}
    </>
  ),
});
