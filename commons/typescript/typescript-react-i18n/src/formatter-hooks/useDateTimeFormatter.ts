/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Imports
 */

import { useCallback } from "react";
import { useLanguage } from "../utility-hooks/useLanguage";

/**
 * Type Declarations
 */

export type Style =
  | "date"
  | "date-and-time"
  | "date-and-time-with-seconds"
  | "date-with-weekday"
  | "date-with-weekday-and-time"
  | "time"
  | "time-with-seconds";

/**
 * Available time formats
 */

const options: Record<Style, Intl.DateTimeFormatOptions> = {
  date: {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  },
  "date-and-time": {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hourCycle: "h23",
    hour: "2-digit",
    minute: "2-digit",
  },
  "date-and-time-with-seconds": {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hourCycle: "h23",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  },
  "date-with-weekday": {
    weekday: "short",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  },
  "date-with-weekday-and-time": {
    weekday: "short",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hourCycle: "h23",
    hour: "2-digit",
    minute: "2-digit",
  },
  time: {
    hourCycle: "h23",
    hour: "2-digit",
    minute: "2-digit",
  },
  "time-with-seconds": {
    hourCycle: "h23",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  },
};

/**
 * Cache for Intl.DateTimeFormat instances
 *
 * Since the instantiation of Intl.DateTimeFormat is expensive, we cache the instances for each language and style
 */

const intlDateTimeFormatCache = new Map<string, Intl.DateTimeFormat>();

function getIntlDateTimeFormat(language: string, style: Style): Intl.DateTimeFormat {
  const key = `${language}.${style}`;

  const intlDateTimeFormat = intlDateTimeFormatCache.get(key);

  if (intlDateTimeFormat) {
    return intlDateTimeFormat;
  }

  const newIntlDateTimeFormat = new Intl.DateTimeFormat(language, options[style]);
  intlDateTimeFormatCache.set(key, newIntlDateTimeFormat);
  return newIntlDateTimeFormat;
}

/**
 * use DateTimeFormat Hook
 *
 * @returns a formatter function that formats a date to a localized string
 */
export function useDateTimeFormatter(style: Style = "date") {
  const language = useLanguage();
  const intlDateTimeFormat = getIntlDateTimeFormat(language, style);

  const formatter = useCallback(
    <T extends Date | null | undefined>(date: T): T extends Date ? string : T => {
      if (date == null) {
        return date as any; // preserve null and undefined, cast is necessary due to conditional return type
      }

      return intlDateTimeFormat.format(date) as any; // cast is necessary due to conditional return type
    },
    [intlDateTimeFormat],
  );

  return formatter;
}
