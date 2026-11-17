/**
 * Parses an ISO date-time string into a Date object
 *
 * @param dateTime - The ISO date-time string to parse
 * @returns Date object
 */
export function parseDateTime(dateTime: string): Date;
export function parseDateTime(dateTime: null): null;
export function parseDateTime(dateTime: undefined): undefined;
export function parseDateTime(dateTime: string | null): Date | null;
export function parseDateTime(dateTime: string | undefined): Date | undefined;
export function parseDateTime(dateTime: string | null | undefined): Date | null | undefined;
export function parseDateTime(dateTime: string | null | undefined): Date | null | undefined {
  if (dateTime === null) return null;
  if (dateTime === undefined) return undefined;

  // Validate strict ISO format using regex
  const regex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$/;

  if (!regex.test(dateTime)) {
    throw new Error(`DateTime must be in ISO format: 'YYYY-MM-DDTHH:mm:ss(.sss)Z' but was [${dateTime}]`);
  }

  const date = new Date(dateTime);

  if (isNaN(date.getTime())) {
    throw new Error("Invalid ISO date-time");
  }

  // Extract components for extra validation
  const [datePart, timePart] = dateTime.replace("Z", "").split("T");
  if (datePart === undefined || timePart === undefined) {
    throw new Error(`DateTime must be in ISO format: 'YYYY-MM-DDTHH:mm:ss(.sss)Z' but was [${dateTime}]`);
  }

  const [year, month, day] = datePart.split("-").map(Number);
  const [hour, minute, second] = timePart.split(/[.:]/).map(Number);

  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() + 1 !== month ||
    date.getUTCDate() !== day ||
    date.getUTCHours() !== hour ||
    date.getUTCMinutes() !== minute ||
    date.getUTCSeconds() !== second
  ) {
    throw new Error("Invalid date-time components");
  }

  return date;
}
