/**
 * Imports
 */

import { capitalizeFirstCharacter } from "@neckar-hub/typescript-utils/converters";
import { parseAsIsoDateTime, useQueryState } from "nuqs";

/**
 * Hook to manage a single instant (date + time) search parameter.
 *
 * The URL value is a full ISO-8601 instant (`yyyy-MM-ddTHH:mm:ss[.SSS]Z`) — the same shape
 * the backend filter accepts directly. Returned `Date` objects carry the picked timestamp;
 * pair with backend filter logic that compares an `Instant` field against the bound.
 *
 * Use this hook (and `useDateTimeFilterSearchParam`) when the underlying DB column is an
 * `Instant`. For calendar-day-only state over a `LocalDate` column, use `useDateSearchParam`.
 *
 * This hook should not be used for filters (i.e. in tables or lists), use the
 * `useDateTimeFilterSearchParam` hook instead.
 */
export function useDateTimeSearchParam(key: string, prefix?: string) {
  const parsedKey = prefix ? `${prefix}${capitalizeFirstCharacter(key)}` : key;
  const [param, setParam] = useQueryState(parsedKey, parseAsIsoDateTime.withOptions({ history: "push" }));

  const value: Date | undefined = param ?? undefined;

  const setValue = (value: Date | undefined) => {
    void setParam(value ?? null);
  };

  return [value, setValue] as const;
}
