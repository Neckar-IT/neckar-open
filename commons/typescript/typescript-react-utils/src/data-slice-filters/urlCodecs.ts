/**
 * Readers of one URL value: an absent or unreadable one is `undefined`, so a hand-edited URL means "no filter".
 */

export function parseInteger(urlValue: string | null | undefined): number | undefined {
  const number = parseNumber(urlValue);
  return number !== undefined && Number.isInteger(number) ? number : undefined;
}

export function parseNumber(urlValue: string | null | undefined): number | undefined {
  if (urlValue === null || urlValue === undefined || urlValue.trim() === "") {
    return undefined;
  }
  const number = Number(urlValue);
  return Number.isFinite(number) ? number : undefined;
}

/**
 * Reads `yyyy-MM-dd` as local midnight of that day, the day a date picker shows in any timezone.
 */
export function parseLocalDate(urlValue: string | null | undefined): Date | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:$|T)/.exec(urlValue ?? "");
  if (match === null) {
    return undefined;
  }
  const [year, month, day] = match.slice(1).map(Number) as [number, number, number];
  const date = new Date(year, month - 1, day);
  // `new Date` rolls an impossible day such as 2026-02-31 into the next month.
  return date.getMonth() === month - 1 && date.getDate() === day ? date : undefined;
}

/** Reads an ISO-8601 instant. */
export function parseInstant(urlValue: string | null | undefined): Date | undefined {
  if (urlValue === null || urlValue === undefined || urlValue === "") {
    return undefined;
  }
  const date = new Date(urlValue);
  return Number.isNaN(date.valueOf()) ? undefined : date;
}
