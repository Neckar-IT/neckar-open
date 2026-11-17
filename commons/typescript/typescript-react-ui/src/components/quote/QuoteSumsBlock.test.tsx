import type { QuoteSums } from "@neckar-hub/typescript-utils/openapi-types";
import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { discountedBookOrderQuote, vatLine } from "./quoteFixtures";
import { QuoteSumsBlock } from "./QuoteSumsBlock";

/** 10.00 € at 7 % and 20.00 € at 19 %, ascending by rate as the backend sends it. */
const sums: QuoteSums = {
  preDiscountNet: 3000,
  discount: 0,
  net: 3000,
  vatBreakdown: [vatLine(1000, 0.07, 70), vatLine(2000, 0.19, 380)],
  gross: 3450,
};

describe("QuoteSumsBlock", () => {
  it("renders net, one tax line per rate and gross, in that order, for an undiscounted quote", () => {
    expect(renderedPairs(sums)).toEqual([
      "Net total|30.00 €",
      "plus VAT 7 % on 10.00 €|0.70 €",
      "plus VAT 19 % on 20.00 €|3.80 €",
      "Gross total|34.50 €",
    ]);
  });

  it("renders the subtotal and the discount above the net of a discounted quote", () => {
    expect(renderedPairs(discountedBookOrderQuote.sums)).toEqual([
      "Subtotal|42.73 €",
      "Discount|-2.14 €",
      "Net total|40.59 €",
      "plus VAT 19 % on 40.59 €|7.71 €",
      "Gross total|48.30 €",
    ]);
  });

  it("renders the tax amount the block states rather than recalculating it", () => {
    // 19 % of 1.00 € is 0.19 €; a stated 0.20 € shows that the component does not calculate.
    const stated: QuoteSums = { preDiscountNet: 100, discount: 0, net: 100, vatBreakdown: [vatLine(100, 0.19, 20)], gross: 120 };

    expect(renderedPairs(stated)).toContain("plus VAT 19 % on 1.00 €|0.20 €");
  });

  it("sets the gross apart from the lines above it", () => {
    const { container } = render(<QuoteSumsBlock sums={sums} />);
    const terms = [...container.querySelectorAll("dt")];

    expect(terms.at(-1)?.className).toContain("font-bold");
    expect(terms[0]?.className).not.toContain("font-bold");
  });
});

/** Every term with its value, joined by `|`, with the non-breaking space before a unit read as a plain one. */
function renderedPairs(quoteSums: QuoteSums): string[] {
  const { container } = render(<QuoteSumsBlock sums={quoteSums} />);

  return [...container.querySelectorAll("dt")].map((term) =>
    `${term.textContent}|${term.nextElementSibling?.textContent ?? ""}`.replaceAll("\u00A0", " "),
  );
}
