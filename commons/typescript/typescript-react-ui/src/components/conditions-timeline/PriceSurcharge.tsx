import { calculateSurcharge } from "@neckar-hub/typescript-utils/money";
import type { PriceWithProfit } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { entityPropertyRenderer } from "../data-display/entity-property-renderer";
import { NumberOutput } from "../data-display/number-output";
import { conversionFactors } from "../quantities/conversionFactors";
import { EuroOutput } from "../quantities/money/euros";

/**
 * Displays what a price pair earns — `+18,00 € (+36,0 %)`: the difference between purchase and
 * selling price, followed by that difference relative to the purchase price.
 *
 * Takes a whole {@link PriceWithProfit}, so it serves the article's part and its accessory alike —
 * and a price pair a form still holds, which satisfies the same shape.
 *
 * Belongs where a label or the purchase price beside it states what the percentage is measured against:
 * a labelled table column, the labelled preview of a form. A surcharge on the purchase price and a trade margin on the selling
 * price describe the same position with different numbers (40 % against 28,6 %), so the figure is only
 * unambiguous next to its name.
 *
 * Selling below the purchase price is rendered in the destructive colour: it is not a smaller profit
 * but a loss, and reads as one. Selling at cost stays a value — `0,00 € (0,0 %)` — and is never
 * collapsed into the missing-value dash. A zero purchase price leaves the percentage undefined; the
 * amount is then shown alone.
 */
export const PriceSurcharge = entityPropertyRenderer("price")<PriceWithProfit>({
  displayName: "PriceSurcharge",
  render: ({ entity }) => {
    const { profit, surchargeRate } = calculateSurcharge(entity);

    return <SurchargeOutput surcharge={profit} surchargeRate={surchargeRate} />;
  },
});

interface SurchargeOutputProps {
  /** Selling minus purchase price, in cents. */
  surcharge: number;
  /** Fraction of the purchase price, `null` where the purchase price is 0. */
  surchargeRate: number | null;
}

/**
 * Renders a surcharge that is already calculated — `+18,00 € (+36,0 %)` — in the way {@link PriceSurcharge}
 * describes.
 */
export function SurchargeOutput({ surcharge, surchargeRate }: SurchargeOutputProps): React.ReactElement {
  return (
    <span className={cn(surcharge < 0 && "text-destructive")}>
      {explicitPlusFor(surcharge)}
      <EuroOutput value={surcharge} />
      {surchargeRate !== null ? (
        <span className="text-muted-foreground">
          {" ("}
          <SurchargeRateOutput surchargeRate={surchargeRate} />
          {")"}
        </span>
      ) : null}
    </span>
  );
}

/**
 * A surcharge rate that is already calculated, `+36,0 %`; the caller colours it.
 */
export function SurchargeRateOutput({
  surchargeRate,
  fractionDigits = 1,
}: {
  surchargeRate: number;
  fractionDigits?: number;
}): React.ReactElement {
  return (
    <>
      {explicitPlusFor(surchargeRate)}
      <NumberOutput
        value={surchargeRate}
        unit="%"
        conversionFactor={conversionFactors.fraction.toPercent}
        fractionDigits={fractionDigits}
      />
    </>
  );
}

/**
 * A leading `+` for a positive figure, nothing otherwise. Negative figures carry their minus from
 * the formatter, and zero needs no sign — only a gain has to be spelled out to read as a direction
 * rather than as a plain amount.
 */
function explicitPlusFor(value: number): string | null {
  return value > 0 ? "+" : null;
}
