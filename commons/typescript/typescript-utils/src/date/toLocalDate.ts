/**
 * Converts a Date to a local date string in the format `YYYY-MM-DD`.
 *
 * @param date
 * @returns string `YYYY-MM-DD`
 */
export function toLocalDate(date: Date): string;
export function toLocalDate(date: null): null;
export function toLocalDate(date: undefined): undefined;
export function toLocalDate(date: Date | null): string | null;
export function toLocalDate(date: Date | undefined): string | undefined;
export function toLocalDate(date: Date | null | undefined): string | null | undefined;
export function toLocalDate(date: Date | null | undefined): string | null | undefined {
  if (date === null) return null;
  if (date === undefined) return undefined;

  const year = String(date.getFullYear());
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
