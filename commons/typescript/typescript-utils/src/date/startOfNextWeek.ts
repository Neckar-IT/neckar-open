import dayjs from "dayjs";
import isoWeek from "dayjs/plugin/isoWeek";

// Registered here rather than by the caller: without the plugin `startOf("isoWeek")` silently
// falls back to a no-op.
dayjs.extend(isoWeek);

/**
 * Local midnight on the Monday of the week after the given date.
 *
 * Deliberately the ISO-8601 week and not `startOf("week")`: the latter follows the globally set
 * dayjs locale, which defaults to Sunday and is switched at runtime by application code.
 *
 * @param date any point in time
 * @returns Date at 00:00:00.000 local time on the following Monday
 */
export function startOfNextWeek(date: Date): Date {
  return dayjs(date).add(1, "week").startOf("isoWeek").toDate();
}
