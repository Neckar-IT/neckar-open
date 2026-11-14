/**
 * Parses a local date string in YYYY-MM-DD format into a Date object (midnight on the given date in local timezone)
 *
 * @param localDate | null | undefined
 * @returns Date | null | undefined
 */
export function parseLocalDate(localDate: string): Date;
export function parseLocalDate(localDate: null): null;
export function parseLocalDate(localDate: undefined): undefined;
export function parseLocalDate(localDate: string | null): Date | null;
export function parseLocalDate(localDate: string | undefined): Date | undefined;
export function parseLocalDate(localDate: string | null | undefined): Date | null | undefined;
export function parseLocalDate(localDate: string | null | undefined): Date | null | undefined {
  if (localDate === null) return null;
  if (localDate === undefined) return undefined;

  // Check basic YYYY-MM-DD format using regex
  const regex = /^\d{4}-\d{2}-\d{2}$/;
  if (!regex.test(localDate)) {
    throw new Error(`Date must be in 'YYYY-MM-DD' format but was: [${localDate}]`);
  }

  // create a local data object (i.e. midnight on the given date in the local timezone)
  const [year, month, day] = localDate.split("-").map(Number);
  if (year === undefined || month === undefined || day === undefined) {
    throw new Error(`Date must be in 'YYYY-MM-DD' format but was: [${localDate}]`);
  }

  const date = new Date(year, month - 1, day);

  // Additional validation: ensure parsed date matches components (avoid invalid dates)
  if (date.getFullYear() !== year || date.getMonth() + 1 !== month || date.getDate() !== day) {
    throw new Error("Invalid date");
  }

  return date;
}
