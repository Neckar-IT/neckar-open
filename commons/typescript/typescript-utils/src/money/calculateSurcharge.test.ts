import { describe, expect, it } from "vitest";
import { calculateSurcharge } from "./calculateSurcharge";

describe("calculateSurcharge", () => {
  it("returns the profit and the rate relative to the purchase price", () => {
    // The example pinned by PriceWithProfitTest on the Kotlin side: 50,00 € purchased, 68,00 € sold.
    expect(calculateSurcharge({ inputPrice: 5000, sellingPrice: 6800 })).toEqual({ profit: 1800, surchargeRate: 0.36 });
  });

  it("returns a zero rate when selling for the purchase price", () => {
    expect(calculateSurcharge({ inputPrice: 5000, sellingPrice: 5000 })).toEqual({ profit: 0, surchargeRate: 0 });
  });

  it("returns negative values when selling below the purchase price", () => {
    expect(calculateSurcharge({ inputPrice: 5000, sellingPrice: 4000 })).toEqual({ profit: -1000, surchargeRate: -0.2 });
  });

  it("returns a null rate for a zero purchase price, keeping the profit", () => {
    expect(calculateSurcharge({ inputPrice: 0, sellingPrice: 6800 })).toEqual({ profit: 6800, surchargeRate: null });
  });

  it("returns a null rate when both prices are zero", () => {
    expect(calculateSurcharge({ inputPrice: 0, sellingPrice: 0 })).toEqual({ profit: 0, surchargeRate: null });
  });
});
