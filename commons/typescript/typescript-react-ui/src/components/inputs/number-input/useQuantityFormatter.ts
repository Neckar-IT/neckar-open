/**
 * Imports
 */

import { useNumberFormatter } from "@neckar-hub/typescript-react-i18n";
import { useCallback } from "react";

/**
 * How a value stored in its base unit is turned into the number the user reads
 */

export interface QuantityFormatOptions {
  fractionDigits?: number | undefined;
  /**
   * Factor from the stored base unit to the displayed one — 0.01 turns cents into euros
   */
  conversionFactor?: number | undefined;
  useGrouping?: boolean | undefined;
}

/**
 * Hook to format a value stored in its base unit the way an input field displays it
 *
 * Every place that renders such a value goes through this hook: the base unit exists only
 * internally, so a render site that formats without the factor shows a hundredfold value.
 */

export function useQuantityFormatter({
  fractionDigits,
  conversionFactor = 1,
  useGrouping,
}: QuantityFormatOptions): (value: number) => string {
  const format = useNumberFormatter(fractionDigits, useGrouping);

  return useCallback((value: number) => format(value * conversionFactor), [format, conversionFactor]);
}
