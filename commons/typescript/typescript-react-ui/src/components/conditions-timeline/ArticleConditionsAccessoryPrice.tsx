import type { PriceWithProfit } from "@neckar-hub/typescript-utils/openapi-types";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import { EuroOutput } from "../quantities/money/euros";

/**
 * Displays the selling price of the accessory supplied with an article — e.g. `210,00 €`.
 *
 * Carries no unit: the accessory belongs to one unit of the article's part, so its price is read
 * against the part's unit. Prices are stored in cents on the wire.
 */
export const ArticleConditionsAccessoryPrice = entityPropertyRenderer("accessory")<PriceWithProfit>({
  displayName: "ArticleConditionsAccessoryPrice",
  render: ({ entity }) => <EuroOutput value={entity.sellingPrice} />,
});
