import type {
  AmountUnit,
  FlatQuoteDetails,
  FlatQuoteLineDetails,
  MoneyWithVat,
  QuoteSums,
} from "@neckar-hub/typescript-utils/openapi-types";

/**
 * Quote fixtures shared by the tests and stories of this folder: a book order of two top-level
 * sections, one of them with a nested section, billed at 19 %.
 */
export function section(position: string, depth: number, headline: string, total: number): FlatQuoteLineDetails {
  return { type: "Section", position, depth, headline, details: null, amount: null, total, ...mandatory(total) };
}

export function item(
  position: string,
  depth: number,
  headline: string,
  amount: number,
  unitPrice: number,
  total: number,
  unit?: AmountUnit,
): FlatQuoteLineDetails {
  return {
    type: "Item",
    position,
    depth,
    headline,
    details: null,
    amount: unit === undefined ? { amount } : { amount, unit },
    unitPrice,
    total,
    ...mandatory(total),
  };
}

/** The bill holds mandatory lines only, so what a line would cost including options is what it costs. */
function mandatory(total: number): Pick<FlatQuoteLineDetails, "totalIncludingOptional" | "optionality" | "vatRates" | "visibility"> {
  return { totalIncludingOptional: total, optionality: "Mandatory", vatRates: [0.19], visibility: "Public" };
}

export function vatLine(net: number, vatRate: number, vatAmount: number): MoneyWithVat {
  return { net, vatRate, vatAmount, gross: net + vatAmount };
}

/** The sum block of an undiscounted bill of `net` cents, all of it taxed at 19 %. */
export function sumsOf(net: number): QuoteSums {
  const vatAmount = Math.round(net * 0.19);

  return { preDiscountNet: net, discount: 0, net, vatBreakdown: [vatLine(net, 0.19, vatAmount)], gross: net + vatAmount };
}

function internal(line: FlatQuoteLineDetails): FlatQuoteLineDetails {
  return { ...line, visibility: "Internal" };
}

function optional(line: FlatQuoteLineDetails, totalIncludingOptional: number): FlatQuoteLineDetails {
  return { ...line, optionality: "Optional", total: 0, totalIncludingOptional };
}

export const bookOrderQuote: FlatQuoteDetails = {
  lines: [
    section("1", 1, "Bücher", 3774),
    { ...item("1.1", 2, "To Kill a Mockingbird", 2, 1747, 3494), details: "Harper Lee, Taschenbuch" },
    section("1.2", 2, "Zubehör", 280),
    item("1.2.1", 3, "Lesezeichen", 1, 280, 280),
    section("2", 1, "Versand (versandkostenfrei ab 50,00 €)", 499),
    item("2.1", 2, "Verpackung", 1, 150, 150),
    item("2.2", 2, "Versand", 1, 349, 349),
  ],
  sums: sumsOf(4273),
};

/** 5 % off 42.73 €: 2.14 € taken off, 40.59 € net, taxed at 19 %. */
export const discountedBookOrderQuote: FlatQuoteDetails = {
  lines: bookOrderQuote.lines,
  sums: { preDiscountNet: 4273, discount: -214, net: 4059, vatBreakdown: [vatLine(4059, 0.19, 771)], gross: 4830 },
};

/** One item whose headline and details are far longer than the description column. */
export const longDescriptionQuote: FlatQuoteDetails = {
  lines: [
    {
      ...item(
        "1",
        1,
        "Lieferung und Montage einer Photovoltaikanlage inklusive Wechselrichter, Speicher und Anschluss an den Zählerschrank",
        1,
        1250000,
        1250000,
      ),
      details:
        "Module Glas-Glas 440 Wp, Aufdachmontage auf Ziegeldach, Kabelweg im Leerrohr, Inbetriebnahme mit Protokoll und Einweisung des Betreibers",
    },
  ],
  sums: sumsOf(1250000),
};

/** The book order with the packaging as an internal line. */
export const internalLineQuote: FlatQuoteDetails = {
  lines: bookOrderQuote.lines.map((line) => (line.headline === "Verpackung" ? internal(line) : line)),
  sums: bookOrderQuote.sums,
};

/** The book order with the bookmark as an optional item and the shipping section as an optional section. */
export const optionalLineQuote: FlatQuoteDetails = {
  lines: bookOrderQuote.lines.map((line) => {
    if (line.headline === "Lesezeichen") return optional(line, 280);
    if (line.position === "2") return optional(line, 499);
    return line;
  }),
  sums: bookOrderQuote.sums,
};

/** A section over a 7 % and a 19 % item, and a section without a mandatory line, so without a rate. */
export const mixedVatRateQuote: FlatQuoteDetails = {
  lines: [
    { ...section("1", 1, "Bücher und Versand", 3000), vatRates: [0.07, 0.19] },
    { ...item("1.1", 2, "Buch", 1, 1000, 1000), vatRates: [0.07] },
    item("1.2", 2, "Versand", 1, 2000, 2000),
    { ...section("2", 1, "Optionales Zubehör", 0), vatRates: [] },
  ],
  sums: {
    preDiscountNet: 3000,
    discount: 0,
    net: 3000,
    vatBreakdown: [vatLine(1000, 0.07, 70), vatLine(2000, 0.19, 380)],
    gross: 3450,
  },
};
