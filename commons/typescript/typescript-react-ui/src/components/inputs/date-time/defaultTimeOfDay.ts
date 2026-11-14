/**
 * Default time-of-day applied automatically the first time the user picks a calendar day
 * (when no `value` is set yet). Once any time is set on the value, picking a different day
 * preserves that existing time — the default only seeds the very first pick.
 *
 * Symbolic shortcuts:
 * - `"start"` → `00:00:00.000` (lower bound, "from" — include the entire day from its start)
 * - `"middle"` → `12:00:00.000` (single-pick — "anywhere on that day")
 * - `"end"` → `23:59:59.999` (upper bound, "to" — include the entire day through its end)
 *
 * Or pass an explicit local-time triple (`{ hours, minutes }`) for any other default —
 * e.g. `{ hours: 8, minutes: 0 }` for "start of business day".
 */
export type DefaultTimeOfDay = "start" | "middle" | "end" | { hours: number; minutes: number; seconds?: number; milliseconds?: number };

/**
 * Local-time `(hours, minutes, seconds, ms)` triple for each symbolic [DefaultTimeOfDay] value.
 * Co-located here so the calendar-click seed and the badge-suppression check read the same
 * source of truth. Object-form values bypass this map and are read directly.
 */
const SYMBOLIC_TIME_COMPONENTS: Record<"start" | "middle" | "end", [number, number, number, number]> = {
  start: [0, 0, 0, 0],
  middle: [12, 0, 0, 0],
  end: [23, 59, 59, 999],
};

export function resolveTimeComponents(defaultTimeOfDay: DefaultTimeOfDay): [number, number, number, number] {
  if (typeof defaultTimeOfDay === "string") {
    return SYMBOLIC_TIME_COMPONENTS[defaultTimeOfDay];
  }
  return [defaultTimeOfDay.hours, defaultTimeOfDay.minutes, defaultTimeOfDay.seconds ?? 0, defaultTimeOfDay.milliseconds ?? 0];
}

export function combineDayAndTime(day: Date, timeSource: Date | undefined, defaultTimeOfDay: DefaultTimeOfDay): Date {
  const result = new Date(day.getFullYear(), day.getMonth(), day.getDate());
  if (timeSource !== undefined) {
    result.setHours(timeSource.getHours(), timeSource.getMinutes(), timeSource.getSeconds(), timeSource.getMilliseconds());
  } else {
    const [h, m, s, ms] = resolveTimeComponents(defaultTimeOfDay);
    result.setHours(h, m, s, ms);
  }
  return result;
}

/**
 * True when the time portion of `date` exactly matches the configured default time-of-day.
 * Used by trigger badges to suppress visual noise: a `from`-trigger at start-of-day or a
 * `to`-trigger at end-of-day shows only the calendar date.
 */
export function matchesDefaultTimeOfDay(date: Date, defaultTimeOfDay: DefaultTimeOfDay): boolean {
  const [h, m, s, ms] = resolveTimeComponents(defaultTimeOfDay);
  return date.getHours() === h && date.getMinutes() === m && date.getSeconds() === s && date.getMilliseconds() === ms;
}
