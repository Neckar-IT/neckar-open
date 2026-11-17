import { act, waitFor } from "@testing-library/react";
import { expect, type Mock } from "vitest";

/** The answer of the stubbed price-overview companion: a part-only overview. */
export const stubbedPriceSummaryResponse = {
  type: "good",
  data: {
    unit: null,
    part: { inputPrice: 2000, sellingPrice: 2500, surcharge: 500, surchargeRate: 0.25 },
    accessory: null,
    labour: null,
    customerPrice: 2500,
  },
};

/**
 * Renders the page, then waits until it has asked the stubbed price-overview companion and applied its
 * answer, so no state update or console call follows the end of the test.
 */
export async function renderAwaitingPriceSummaryCompanion<Rendered>(
  calculatePriceSummary: Mock,
  render: () => Rendered,
): Promise<Rendered> {
  calculatePriceSummary.mockClear();
  const rendered = render();
  await waitFor(() => expect(calculatePriceSummary).toHaveBeenCalled());
  await act(async () => {});
  return rendered;
}
