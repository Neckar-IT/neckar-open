import type { PriceWithProfit } from "../openapi-types";

/**
 * What a priced position earns: the absolute difference between selling and purchase price, and
 * that difference relative to the purchase price.
 */
export interface SurchargeFigures {
  /** Selling price minus purchase price, in cents. Negative when selling below the purchase price. */
  profit: number;
  /**
   * The {@link profit} relative to the purchase price, as a fraction (`0.36`, not `36`) — the shape
   * `conversionFactors.fraction.toPercent` converts for display.
   *
   * `null` for a zero purchase price: a surcharge on nothing is undefined. Callers show the
   * {@link profit} alone in that case.
   */
  surchargeRate: number | null;
}

/**
 * Derives the surcharge of a price pair.
 *
 * The single place that knows the definition. `PriceWithProfit.profit` is its counterpart in the
 * domain but is not serialized; both operands arrive as exact integer cents, so the difference is
 * computed here rather than shipped.
 *
 * The rate is the inverse of the domain's `PriceWithProfit.surcharge`, which builds the selling
 * price as `inputPrice * (1 + surcharge)` — so the percentage shown is the one used to calculate.
 * `PriceWithProfitTest` pins that factory against the same example this module's test uses.
 */
export function calculateSurcharge({ inputPrice, sellingPrice }: PriceWithProfit): SurchargeFigures {
  const profit = sellingPrice - inputPrice;
  return { profit, surchargeRate: inputPrice === 0 ? null : profit / inputPrice };
}
