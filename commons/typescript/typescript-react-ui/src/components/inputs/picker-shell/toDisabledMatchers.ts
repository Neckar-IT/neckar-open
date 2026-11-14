import { type Matcher } from "react-day-picker";

/**
 * The calendar matchers that disable every day before `min` and after `max`.
 */
export function toDisabledMatchers(min: Date | undefined, max: Date | undefined): Matcher[] {
  const matchers: Matcher[] = [];
  if (min !== undefined) matchers.push({ before: min });
  if (max !== undefined) matchers.push({ after: max });
  return matchers;
}
