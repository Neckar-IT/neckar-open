/**
 * Imports
 */

import { useCallback } from "react";

/**
 * Formats a number of hours as `hours:minutes`, rounded to whole minutes: `1.5` becomes `1:30`, `0.05` becomes `0:03`.
 */
function formatHoursAndMinutes(hours: number): string {
  const totalMinutes = Math.round(hours * 60);
  const wholeHours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes - wholeHours * 60;

  return `${wholeHours.toString()}:${minutes.toString().padStart(2, "0")}`;
}

/**
 * Hook to format a number of hours as `hours:minutes`, rounded to whole minutes.
 * The format does not depend on the language.
 *
 * @returns a formatter function that formats hours to a `hours:minutes` string
 */
export function useHoursAndMinutesFormatter(): (hours: number) => string {
  return useCallback((hours: number) => formatHoursAndMinutes(hours), []);
}
