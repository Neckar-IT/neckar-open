/**
 * Conversion helpers between the wire format of an hours-scale duration (an ISO-8601 duration
 * string such as `"PT8H30M"`, the format Kotlin's `kotlin.time.Duration` serializes to) and a
 * plain hours number used by forms and inputs.
 *
 * Used for the installation time of an article (`ArticleConditions.installationTime`), which is
 * an hours-scale magnitude: an assembly spanning several days legitimately exceeds 24 h.
 *
 * For sub-minute magnitudes (switchover times and the like) the seconds-level helpers of the
 * consuming project are the better fit — this pair rounds to whole seconds.
 */

const ISO_DURATION_PATTERN = /^(-)?PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?$/;

const SECONDS_PER_HOUR = 3600;
const SECONDS_PER_MINUTE = 60;

/**
 * Parses an ISO-8601 duration string into hours. Returns null when absent or unparseable.
 *
 * Handles the components Kotlin emits for hours-scale durations: `"PT8H"`, `"PT8H30M"`,
 * `"PT45M"`, `"PT2H15M30S"`, and fractional components such as `"PT8.5H"`.
 */
export function parseIsoDurationHours(isoDuration: string | null | undefined): number | null {
  if (isoDuration == null) return null;

  const match = ISO_DURATION_PATTERN.exec(isoDuration);
  if (match === null) return null;

  const [, sign, hours, minutes, seconds] = match;
  if (hours === undefined && minutes === undefined && seconds === undefined) return null;

  const totalHours =
    (hours === undefined ? 0 : Number.parseFloat(hours)) +
    (minutes === undefined ? 0 : Number.parseFloat(minutes) / 60) +
    (seconds === undefined ? 0 : Number.parseFloat(seconds) / SECONDS_PER_HOUR);

  return sign === "-" ? -totalHours : totalHours;
}

/**
 * Formats an hours number as an ISO-8601 duration string (e.g. `8.5` → `"PT8H30M"`).
 *
 * Components that are zero are omitted; a zero duration becomes `"PT0S"`. Fractions below one
 * second are rounded away — an installation time is never estimated that precisely.
 */
export function formatIsoDurationFromHours(hours: number): string {
  const totalSeconds = Math.round(hours * SECONDS_PER_HOUR);
  if (totalSeconds === 0) return "PT0S";

  const sign = totalSeconds < 0 ? "-" : "";
  const absoluteSeconds = Math.abs(totalSeconds);

  const wholeHours = Math.floor(absoluteSeconds / SECONDS_PER_HOUR);
  const wholeMinutes = Math.floor((absoluteSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
  const remainingSeconds = absoluteSeconds % SECONDS_PER_MINUTE;

  const components = [
    wholeHours === 0 ? "" : `${wholeHours}H`,
    wholeMinutes === 0 ? "" : `${wholeMinutes}M`,
    remainingSeconds === 0 ? "" : `${remainingSeconds}S`,
  ].join("");

  return `${sign}PT${components}`;
}
