/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { toLocalDate } from "@neckar-hub/typescript-utils/date";
import { createParser, useQueryState } from "nuqs";
import { parseLocalDate } from "../data-slice-filters/urlCodecs";

/**
 * nuqs parser for an ISO-8601 calendar date (`yyyy-MM-dd`) that round-trips through the
 * caller's **local** timezone — the URL string is exactly the calendar day a `DatePicker`
 * shows, regardless of how far the local timezone sits from UTC.
 *
 * The default `parseAsIsoDate` from nuqs serializes via `Date.toISOString().slice(0, 10)`,
 * which converts to UTC first and shifts the calendar day by ±1 for non-UTC timezones.
 * For a date-picker filter that tells callers "the day the user pointed at", the user's
 * day must win — hence local components on both ends.
 */
const parseAsLocalDate = createParser<Date>({
  parse: (value) => parseLocalDate(value) ?? null,
  serialize: (date) => toLocalDate(date),
  eq: (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate(),
});

/**
 * Calendar-date search parameter for `LocalDate` columns (rare — see ADR 0150). For the
 * normal `Instant` case, use `useDateTimeSearchParam`. For filters in tables/lists, use
 * `useDateFilterSearchParam`.
 *
 * URL value is `yyyy-MM-dd` in the caller's local timezone; returned `Date` is anchored at
 * local midnight on that day.
 */

export function useDateSearchParam(key: string, prefix?: string) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const [param, setParam] = useQueryState(parsedKey, parseAsLocalDate.withOptions({ history: "push" }));

  const value: Date | undefined = param ?? undefined;

  const setValue = (value: Date | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
