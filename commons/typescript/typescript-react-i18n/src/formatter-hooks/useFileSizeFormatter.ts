/**
 * Imports
 */

import { useCallback } from "react";
import { useMaxFractionDigitsFormatter } from "./useMaxFractionDigitsFormatter";
import { useNumberFormatter } from "./useNumberFormatter";

/**
 * The units a file size is shown in, each 1024 times the one before: `1 KB` is 1024 bytes.
 * The abbreviations are the same in every language.
 */
const fileSizeUnits = ["B", "KB", "MB", "GB", "TB"] as const;

const bytesPerStep = 1024;

/**
 * Hook to format a number of bytes as a localized file size in the largest unit in which it is at least 1.
 *
 * The units are `B`, `KB`, `MB`, `GB` and `TB`, each 1024 times the one before. The number shows at most one
 * fraction digit and no trailing zeros: `2560` renders as `2.5 KB` under `en` and `2,5 KB` under `de`, `512` as `512 B`.
 *
 * @returns a formatter function that formats a number of bytes (not negative) to a localized string
 */
export function useFileSizeFormatter(): (bytes: number) => string {
  const formatWholeNumber = useNumberFormatter(0);
  const formatRoundedNumber = useMaxFractionDigitsFormatter(1);

  return useCallback(
    (bytes: number) => {
      if (!Number.isFinite(bytes) || bytes < 0) {
        throw new Error(`A file size must be a finite number of bytes that is not negative, but was <${bytes}>`);
      }

      let unitIndex = 0;
      let value = bytes;
      // Rounding to one digit can carry the value to 1024 of its unit (1023.97 KB); that belongs in the next one.
      while (unitIndex < fileSizeUnits.length - 1 && Math.round(value * 10) / 10 >= bytesPerStep) {
        value /= bytesPerStep;
        unitIndex++;
      }

      const unit = fileSizeUnits[unitIndex] ?? "B";
      // A byte count is whole
      const number = unitIndex === 0 ? formatWholeNumber(value) : formatRoundedNumber(value);
      return `${number} ${unit}`;
    },
    [formatWholeNumber, formatRoundedNumber],
  );
}
