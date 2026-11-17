import dayjs from "dayjs";

/**
 * Local midnight on the first day of the month after the given date.
 *
 * The day-of-month clamping of `add(1, "month")` (31 January → 28 February) is irrelevant here:
 * `startOf("month")` discards the day afterwards.
 *
 * @param date any point in time
 * @returns Date at 00:00:00.000 local time on the first of the following month
 */
export function startOfNextMonth(date: Date): Date {
  return dayjs(date).add(1, "month").startOf("month").toDate();
}
