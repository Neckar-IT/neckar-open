/**
 * Conversion helpers between the wire format of a seconds-scale duration (an ISO-8601 duration
 * string such as `"PT0.050S"`, the format Kotlin's `kotlin.time.Duration` serializes to) and a
 * plain seconds number used by forms and inputs.
 *
 * The seconds counterpart of `isoDurationHours`: a switchover time of 50 ms rounds to nothing in
 * whole seconds, so this pair keeps milliseconds.
 */

const ISO_DURATION_PATTERN = /^(-)?PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?$/;

const SECONDS_PER_HOUR = 3600;
const SECONDS_PER_MINUTE = 60;
const MILLISECONDS_PER_SECOND = 1000;

/**
 * Parses an ISO-8601 duration string into seconds. Returns null when absent or unparseable.
 *
 * Handles every component Kotlin emits below a day: `"PT0.050S"`, `"PT30S"`, `"PT1M"`,
 * `"PT1M30S"`, `"PT2H"`.
 */
export function parseIsoDurationSeconds(isoDuration: string | null | undefined): number | null {
  if (isoDuration == null) return null;

  const match = ISO_DURATION_PATTERN.exec(isoDuration);
  if (match === null) return null;

  const [, sign, hours, minutes, seconds] = match;
  if (hours === undefined && minutes === undefined && seconds === undefined) return null;

  const totalSeconds =
    (hours === undefined ? 0 : Number.parseFloat(hours) * SECONDS_PER_HOUR) +
    (minutes === undefined ? 0 : Number.parseFloat(minutes) * SECONDS_PER_MINUTE) +
    (seconds === undefined ? 0 : Number.parseFloat(seconds));

  return sign === "-" ? -totalSeconds : totalSeconds;
}

/**
 * Formats a seconds number as an ISO-8601 duration string (e.g. `0.05` → `"PT0.05S"`).
 *
 * Rounded to whole milliseconds, so the text never falls back to an exponent (`1e-7`) that no
 * duration parser reads.
 */
export function formatIsoDurationFromSeconds(seconds: number): string {
  const milliseconds = Math.round(seconds * MILLISECONDS_PER_SECOND);
  if (milliseconds === 0) return "PT0S";

  const sign = milliseconds < 0 ? "-" : "";
  return `${sign}PT${(Math.abs(milliseconds) / MILLISECONDS_PER_SECOND).toString()}S`;
}
