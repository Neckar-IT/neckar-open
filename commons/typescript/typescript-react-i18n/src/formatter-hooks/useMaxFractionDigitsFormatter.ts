/**
 * Imports
 */

import { useCallback } from "react";
import { useLanguage } from "../utility-hooks/useLanguage";

const intlNumberFormatCache = new Map<string, Intl.NumberFormat>();

function getIntlNumberFormat(language: string, maximumFractionDigits: number): Intl.NumberFormat {
  const key = `${language}.max${maximumFractionDigits.toString()}`;

  const cached = intlNumberFormatCache.get(key);
  if (cached) {
    return cached;
  }

  const created = new Intl.NumberFormat(language, { style: "decimal", minimumFractionDigits: 0, maximumFractionDigits });
  intlNumberFormatCache.set(key, created);
  return created;
}

/**
 * Hook to format a number to a localized string that is rounded to at most `maximumFractionDigits` digits.
 * Trailing zeros are not padded: `2` stays `2`, `1.3333` becomes `1.33`.
 *
 * @param maximumFractionDigits - The most fraction digits the formatted number shows
 * @returns a formatter function that formats a number to a localized string
 */
export function useMaxFractionDigitsFormatter(maximumFractionDigits: number): (value: number) => string {
  const language = useLanguage();

  const intlNumberFormat = getIntlNumberFormat(language, maximumFractionDigits);

  return useCallback((value: number) => intlNumberFormat.format(value), [intlNumberFormat]);
}
