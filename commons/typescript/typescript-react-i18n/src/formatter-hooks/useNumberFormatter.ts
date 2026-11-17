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

function getIntlNumberFormat(language: string, fractionDigits?: number, useGrouping?: boolean): Intl.NumberFormat {
  const groupingKey = useGrouping === false ? ".noGroup" : "";
  const key = fractionDigits !== undefined ? `${language}.${fractionDigits.toString()}${groupingKey}` : `${language}${groupingKey}`;

  const intlNumberFormat = intlNumberFormatCache.get(key);

  if (intlNumberFormat) {
    return intlNumberFormat;
  }

  const newIntlNumberFormat = new Intl.NumberFormat(language, {
    style: "decimal",
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
    useGrouping: useGrouping ?? true,
  });
  intlNumberFormatCache.set(key, newIntlNumberFormat);
  return newIntlNumberFormat;
}

/**
 * Hook to format a number to a localized string
 *
 * @param fractionDigits - Number of fraction digits to use (will use browser default if not specified)
 * @param useGrouping - Whether to insert the locale's grouping separator; `true` groups every locale
 *   at a thousand, including those that would leave four-digit numbers ungrouped by default. Pass
 *   `false` for values that are identifiers rather than quantities, e.g. a year or a postal code.
 * @returns a formatter function that formats a number to a localized string
 */

export function useNumberFormatter(fractionDigits?: number, useGrouping?: boolean): (value: number) => string {
  const language = useLanguage();

  const intlNumberFormat = getIntlNumberFormat(language, fractionDigits, useGrouping);

  const formatter = useCallback(
    (value: number) => {
      return intlNumberFormat.format(value);
    },
    [intlNumberFormat],
  );

  return formatter;
}
