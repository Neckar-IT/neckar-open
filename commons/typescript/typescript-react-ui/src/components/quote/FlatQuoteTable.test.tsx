import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { AmountUnit, type FlatQuoteDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { render } from "@test/test-utils";
import { render as renderPlain } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FlatQuoteTable } from "./FlatQuoteTable";
import {
  bookOrderQuote,
  internalLineQuote,
  item,
  longDescriptionQuote,
  mixedVatRateQuote,
  optionalLineQuote,
  sumsOf,
} from "./quoteFixtures";

describe("FlatQuoteTable", () => {
  it("renders every line of the quote in one table, in the order of the lines", () => {
    const { container } = render(<FlatQuoteTable quote={bookOrderQuote} />);

    expect(container.querySelectorAll("table")).toHaveLength(1);
    expect(renderedRows().map((row) => row.split("|")[0])).toEqual(["1", "1.1", "1.2", "1.2.1", "2", "2.1", "2.2"]);
  });

  it("renders position, VAT rate, amount, unit price and line total of every item", () => {
    const rows = renderedRows();

    expect(rows).toContain("2.1|Verpackung|19 %|1 pcs.|1.50 €|1.50 €");
    expect(rows).toContain("1.2.1|Lesezeichen|19 %|1 pcs.|2.80 €|2.80 €");
  });

  it("shows the unit behind the amount of every AmountUnit", () => {
    expect(
      amountCells(
        [12.5, AmountUnit.Meters],
        [3.456, AmountUnit.SquareMeters],
        [0.75, AmountUnit.KiloMeters],
        [1.5, AmountUnit.Hours],
        [2, AmountUnit.None],
      ),
    ).toEqual(["12.5 m", "3.46 m²", "0.75 km", "1:30 h", "2 pcs."]);
  });

  it("shows a missing unit like AmountUnit.None", () => {
    expect(amountCells([2, undefined])).toEqual(["2 pcs."]);
  });

  it("rounds to at most two fraction digits without padding zeros", () => {
    expect(amountCells([1.3333333, AmountUnit.None], [2, AmountUnit.Meters])).toEqual(["1.33 pcs.", "2 m"]);
  });

  it("names the unit and the decimal separator in the language of the session", () => {
    const lines = [
      item("1", 1, "Kabel", 1.3333333, 100, 100),
      item("2", 1, "Rohr", 12.5, 100, 100, AmountUnit.Meters),
      item("3", 1, "Montage", 1.5, 100, 100, AmountUnit.Hours),
    ];
    const { container } = renderPlain(
      <I18nProvider options={{ lng: "de" }}>
        <FlatQuoteTable quote={{ lines, sums: sumsOf(300) }} />
      </I18nProvider>,
    );

    expect([...container.querySelectorAll("tbody tr td:nth-child(4)")].map((cell) => cell.textContent?.replaceAll("\u00A0", " "))).toEqual([
      "1,33 Stck.",
      "12,5 m",
      "1:30 Std.",
    ]);
  });

  it("renders the details of a line below its headline", () => {
    expect(renderedRows()).toContain("1.1|To Kill a MockingbirdHarper Lee, Taschenbuch|19 %|2 pcs.|17.47 €|34.94 €");
  });

  it("gives a section its total but neither an amount nor a unit price", () => {
    expect(renderedRows()).toContain("1|Bücher|19 %|||37.74 €");
  });

  it("renders a section row and an item row with the same classes", () => {
    const { container } = render(<FlatQuoteTable quote={bookOrderQuote} />);
    const [section, item] = [...container.querySelectorAll("tbody tr")];

    expect(section?.className).toBe(item?.className);
    expect(section?.className).not.toContain("font-semibold");
    expect(section?.className).not.toContain("bg-muted/60");
  });

  it("emphasizes the headline of every row, section and item alike", () => {
    const { container } = render(<FlatQuoteTable quote={bookOrderQuote} />);
    const rows = [...container.querySelectorAll("tbody tr")];

    expect(rows.length).toBeGreaterThan(0);
    for (const row of rows) {
      expect(row.querySelector("span.font-medium")).not.toBeNull();
    }
  });

  it("aligns the headers of VAT rate, amount, unit price and line total to the right", () => {
    const { container } = render(<FlatQuoteTable quote={bookOrderQuote} />);
    const headers = [...container.querySelectorAll("thead th")];
    const rightAligned = headers.filter((header) => header.querySelector("div.justify-end") !== null);

    expect(rightAligned).toHaveLength(4);
    expect(headers.slice(-4)).toEqual(rightAligned);
  });

  it("lists every rate of a line in ascending order and leaves the cell empty for a line without one", () => {
    expect(renderedRows(mixedVatRateQuote)).toEqual([
      "1|Bücher und Versand|7 % / 19 %|||30.00 €",
      "1.1|Buch|7 %|1 pcs.|10.00 €|10.00 €",
      "1.2|Versand|19 %|1 pcs.|20.00 €|20.00 €",
      "2|Optionales Zubehör||||0.00 €",
    ]);
  });

  it("names the VAT rate in the narrow card view too", () => {
    const { container } = render(<FlatQuoteTable quote={mixedVatRateQuote} />);

    expect(container.querySelector(".\\@3xl\\:hidden")?.textContent).toContain("7 % / 19 %");
  });

  it("indents a position number by one step per level below the top", () => {
    expect(indentsOf([1, 2, 3])).toEqual(["0rem", "0.75rem", "1.5rem"]);
  });

  it("caps the indent, so a deeply nested position number stays inside its column", () => {
    expect(indentsOf([5, 6, 7])).toEqual(["3rem", "3rem", "3rem"]);
  });

  it("renders the sum block below the table rather than as lines of it", () => {
    const { getByText } = render(<FlatQuoteTable quote={bookOrderQuote} />);

    expect(getByText("Net total").closest("dl")?.textContent).toContain("42.73 €");
    expect(renderedRows().at(-1)).toBe("2.2|Versand|19 %|1 pcs.|3.49 €|3.49 €");
  });

  it("lets the headline cell wrap its headline and details, and no other cell", () => {
    const { container } = render(<FlatQuoteTable quote={longDescriptionQuote} />);
    const cells = [...container.querySelectorAll("tbody tr td")];

    expect(cells.map((cell) => cell.firstElementChild?.classList.contains("whitespace-normal"))).toEqual([
      false,
      true,
      false,
      false,
      false,
      false,
    ]);
  });

  it("greys out an internal row and leaves a public row as it is", () => {
    const { container } = render(<FlatQuoteTable quote={internalLineQuote} />);
    const greyedOut = [...container.querySelectorAll("tbody tr.text-muted-foreground")];

    expect(greyedOut).toHaveLength(1);
    expect(greyedOut[0]?.textContent).toContain("Verpackung");
  });

  it("marks an internal row with an icon that explains the greying out, and no public row", () => {
    const { container } = render(<FlatQuoteTable quote={internalLineQuote} />);
    const marked = [...container.querySelectorAll("tbody tr")].filter((row) => row.querySelector("svg[aria-label]") !== null);

    expect(marked).toHaveLength(1);
    expect(marked[0]?.textContent).toContain("Verpackung");
    expect(marked[0]?.querySelector("svg")?.getAttribute("aria-label")).toBe(
      "Internal line: the customer does not see it, and its price is included in the parent line.",
    );
  });

  it("marks an optional section and an optional item with an icon that explains the option, and no mandatory row", () => {
    const { container } = render(<FlatQuoteTable quote={optionalLineQuote} />);
    const marked = [...container.querySelectorAll("tbody tr")].filter((row) => row.querySelector("svg[aria-label]") !== null);

    expect(marked.map((row) => row.querySelector("td:nth-child(2)")?.textContent)).toEqual([
      "Lesezeichen",
      "Versand (versandkostenfrei ab 50,00 €)",
    ]);
    for (const row of marked) {
      expect(row.querySelector("svg")?.getAttribute("aria-label")).toBe(
        "Optional item: not included in the total, can be added on request",
      );
    }
  });

  it("shows no optional icon on a quote of mandatory rows", () => {
    const { container } = render(<FlatQuoteTable quote={bookOrderQuote} />);

    expect(container.querySelector("tbody svg[aria-label]")).toBeNull();
  });

  it("explains the optional icon in a tooltip on keyboard focus", async () => {
    const { container, findByRole } = render(<FlatQuoteTable quote={optionalLineQuote} />);

    container.querySelector<HTMLElement>("tbody svg[aria-label]")?.focus();

    expect((await findByRole("tooltip")).textContent).toContain("Optional item: not included in the total, can be added on request");
  });

  it("names the position number in the narrow card view too", () => {
    const { container } = render(<FlatQuoteTable quote={bookOrderQuote} />);
    const cardView = container.querySelector(".\\@3xl\\:hidden");

    expect(cardView?.textContent).toContain("1.1");
    expect(cardView?.querySelector("table")).toBeNull();
  });
});

/** Every desktop table row, cells joined by `|`, with the non-breaking space before a unit read as a plain one. */
function renderedRows(quote: FlatQuoteDetails = bookOrderQuote): string[] {
  const { container } = render(<FlatQuoteTable quote={quote} />);

  return [...container.querySelectorAll("tbody tr")].map((row) =>
    [...row.querySelectorAll("td")]
      .map((cell) => cell.textContent ?? "")
      .join("|")
      .replaceAll("\u00A0", " "),
  );
}

/** The left padding of every position cell, for a quote of one item per given depth. */
function indentsOf(depths: number[]): string[] {
  const lines = depths.map((depth, index) => item(`${index + 1}`, depth, `Position ${index + 1}`, 1, 100, 100));
  const { container } = render(<FlatQuoteTable quote={{ lines, sums: sumsOf(300) }} />);

  return [...container.querySelectorAll<HTMLElement>("tbody tr td:first-child span")].map((cell) => cell.style.paddingLeft);
}

/** The text of the amount cell of one item per given `[amount, unit]`. */
function amountCells(...amounts: [number, AmountUnit | undefined][]): string[] {
  const lines = amounts.map(([amount, unit], index) => item(`${index + 1}`, 1, `Position ${index + 1}`, amount, 100, 100, unit));
  const { container } = render(<FlatQuoteTable quote={{ lines, sums: sumsOf(100) }} />);

  return [...container.querySelectorAll("tbody tr td:nth-child(4)")].map((cell) => (cell.textContent ?? "").replaceAll("\u00A0", " "));
}
