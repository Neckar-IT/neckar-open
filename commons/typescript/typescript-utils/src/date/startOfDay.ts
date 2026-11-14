import dayjs from "dayjs";

/**
 * Local midnight on the day of the given date.
 *
 * @param date any point in time
 * @returns Date at 00:00:00.000 local time
 */
export function startOfDay(date: Date): Date {
  return dayjs(date).startOf("day").toDate();
}
