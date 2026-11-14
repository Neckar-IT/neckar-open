/**
 * Imports
 */

import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { useCallback } from "react";
import { useLanguage } from "../utility-hooks/useLanguage";

/**
 * Cache for Intl.NumberFormat instances
 *
 * Since the instantiation of Intl.NumberFormat is expensive, we cache the instances for each language and style
 */

const intlNumberFormatCache = new Map<string, Intl.NumberFormat>();

function getIntlNumberFormat(language: string, currency: "EUR" | "USD"): Intl.NumberFormat {
  const key = `${language}.${currency}`;

  const intlNumberFormat = intlNumberFormatCache.get(key);

  if (intlNumberFormat) {
    return intlNumberFormat;
  }

  const newIntlNumberFormat = new Intl.NumberFormat(language, {
    style: "currency",
    currency,
    currencyDisplay: "narrowSymbol",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  intlNumberFormatCache.set(key, newIntlNumberFormat);
  return newIntlNumberFormat;
}

/**
 * Options for the useCurrencyFormatter hook
 */

export interface UseCurrencyFormatterOptions {
  currency?: "EUR" | "USD";
  emptyPlaceholder?: string;
}

/**
 * useCurrencyFormatter Hook
 *
 * @returns {Function} - a formatter function that takes in euros or dollars and returns a localized string
 */

export function useCurrencyFormatter(options?: UseCurrencyFormatterOptions): (amount: number | undefined) => string {
  const { currency = "EUR", emptyPlaceholder = "" } = options ?? {};

  const language = useLanguage();
  const intlNumberFormat = getIntlNumberFormat(language, currency);

  const formatter = useCallback(
    (amount: number | undefined) => {
      if (isNotDefined(amount)) {
        return emptyPlaceholder;
      }

      return (
        intlNumberFormat
          .format(amount)
          // Intl.NumberFormat uses whitespace characters instead of regular spaces, so we replace them here
          .replace(/\u00a0/g, " ")
      );
    },
    [intlNumberFormat, emptyPlaceholder],
  );

  return formatter;
}
