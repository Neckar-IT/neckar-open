/**
 * Imports
 */

import { useCallback } from "react";
import { useLanguage } from "../utility-hooks/useLanguage";

/**
 * Cache for Intl.NumberFormat instances
 *
 * Since the instantiation of Intl.NumberFormat is expensive, we cache the instances for each language and style
 */

const intlNumberFormatCache = new Map<string, Intl.NumberFormat>();

function getIntlNumberFormat(language: string, fractionDigits?: number): Intl.NumberFormat {
  const key = fractionDigits !== undefined ? `${language}.${fractionDigits.toString()}` : language;

  const intlNumberFormat = intlNumberFormatCache.get(key);

  if (intlNumberFormat) {
    return intlNumberFormat;
  }

  const newIntlNumberFormat = new Intl.NumberFormat(language, {
    style: "percent",
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
  intlNumberFormatCache.set(key, newIntlNumberFormat);
  return newIntlNumberFormat;
}

/**
 * Hook to format a ratio as a localized percentage.
 *
 * The formatter takes the ratio, not the percentage value: `0.1234` renders as `12%` under `en` and
 * `12 %` under `de`. Passing `12` yields `1200%`.
 *
 * @param fractionDigits - fraction digits of the rendered percentage, browser default if omitted
 * @returns a formatter function that formats a ratio to a localized percentage string
 */

export function usePercentageFormatter(fractionDigits?: number): (percentage: number) => string {
  const language = useLanguage();

  const intlNumberFormat = getIntlNumberFormat(language, fractionDigits);

  const formatter = useCallback(
    (percentage: number) => {
      return (
        intlNumberFormat
          .format(percentage)
          // Intl.NumberFormat uses whitespace characters instead of regular spaces, so we replace them here
          .replace(/\u00a0/g, " ")
      );
    },
    [intlNumberFormat],
  );

  return formatter;
}
