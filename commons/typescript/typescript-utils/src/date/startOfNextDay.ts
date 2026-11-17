import dayjs from "dayjs";

/**
 * Local midnight on the day after the given date.
 *
 * @param date any point in time
 * @returns Date at 00:00:00.000 local time on the following day
 */
export function startOfNextDay(date: Date): Date {
  return dayjs(date).add(1, "day").startOf("day").toDate();
}
