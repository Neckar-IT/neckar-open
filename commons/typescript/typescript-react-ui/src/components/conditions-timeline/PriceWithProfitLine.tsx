import { calculateSurcharge } from "@neckar-hub/typescript-utils/money";
import type { PriceWithProfit } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { SurchargeRateOutput } from "./PriceSurcharge";

interface PriceWithProfitLineProps {
  price: PriceWithProfit;
  /**
   * Renders one amount of the pair. The call site supplies it because the unit the pair is priced
   * by is the owning record's, not this component's: a travel cost reads as `1,30 €/km`, a
   * per-piece article as `1,30 €`, and both are the same pair of cents on the wire.
   */
  renderAmount: (value: number) => React.ReactNode;
  className?: string;
}

/**
 * Condensed one-line rendering of a price pair a record holds FLAT — `1,30 €/km → 1,82 €/km
 * (+40,0 %)`: what it costs, what it is sold for, and what the two earn.
 *
 * Unlike {@link ArticleConditionsLine}, which shows the selling price alone, this line states the
 * surcharge — because it can. The percentage is only unambiguous next to what it is measured
 * against (a surcharge on the purchase price and a trade margin on the selling price describe the
 * same position with different numbers), and here the purchase price stands two amounts to its
 * left. The overview line of a catalog article has no room for the pair and therefore none for the
 * figure either.
 *
 * The surcharge is derived on every render and never stored: it is the difference between two
 * numbers the record maintains, and a third would be free to disagree with them. Only the rate is
 * shown, not the absolute profit — that one is the difference of the two amounts already on the
 * line, and repeating it would state the same thing twice. A zero purchase price leaves the
 * percentage undefined and the line then shows the two amounts alone.
 */
export function PriceWithProfitLine({ price, renderAmount, className }: PriceWithProfitLineProps): React.ReactElement {
  const { surchargeRate } = calculateSurcharge(price);

  return (
    <span className={cn("flex min-w-0 flex-wrap items-baseline gap-x-2", className)}>
      {renderAmount(price.inputPrice)}
      <span className="text-muted-foreground">→</span>
      {renderAmount(price.sellingPrice)}

      {surchargeRate !== null ? (
        <span className={cn("text-xs", surchargeRate < 0 ? "text-destructive" : "text-muted-foreground")}>
          {"("}
          <SurchargeRateOutput surchargeRate={surchargeRate} />
          {")"}
        </span>
      ) : null}
    </span>
  );
}
