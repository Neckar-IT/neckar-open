/** The year and month fields a form edits; `it.neckar.financial.payment.card.ExpiryDate` travels as the ISO `YYYY-MM` string. */
export interface ExpiryDateParts {
  year: number;
  month: number;
}

export function parseExpiryDate(iso: string): ExpiryDateParts {
  const [yearPart, monthPart] = iso.split("-");
  return { year: Number(yearPart), month: Number(monthPart) };
}

export function formatExpiryDate({ year, month }: ExpiryDateParts): string {
  return `${String(year).padStart(4, "0")}-${String(month).padStart(2, "0")}`;
}

/** Formats the ISO `YYYY-MM` wire form for display as `MM/YYYY`. */
export function formatExpiryDateForDisplay(iso: string): string {
  const { year, month } = parseExpiryDate(iso);
  return `${String(month).padStart(2, "0")}/${year}`;
}
