import { useCallback } from "react";
import { useDateTimeFormatter } from "./useDateTimeFormatter";

/**
 * Formats an ISO instant for display as a "valid from" / "effective from" value.
 *
 * Adaptive: if the stored instant falls exactly on local midnight, only the date is shown.
 * If it has a non-midnight time component, the time is appended so the stored value is
 * never silently hidden.
 *
 * Delegates to {@link useDateTimeFormatter} with styles `date` and `date-and-time`.
 *
 * @returns a formatter function that takes an ISO instant string and returns the formatted value
 */
export function useValidFromDateTimeFormatter(): (isoInstant: string) => string {
  const formatDate = useDateTimeFormatter("date");
  const formatDateAndTime = useDateTimeFormatter("date-and-time");

  return useCallback(
    (isoInstant: string): string => {
      const date = new Date(isoInstant);
      const isLocalMidnight = date.getHours() === 0 && date.getMinutes() === 0 && date.getSeconds() === 0 && date.getMilliseconds() === 0;
      return isLocalMidnight ? formatDate(date) : formatDateAndTime(date);
    },
    [formatDate, formatDateAndTime],
  );
}
