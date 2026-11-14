import { parseNumber } from "@neckar-hub/typescript-react-utils/parsers";
import { useCallback } from "react";
import { useDecimalSeparator } from "../utility-hooks/useNumberSeparators";

/**
 * Parses user input written in the current locale's number format — the inverse of
 * `useNumberFormatter`.
 *
 * Input is salvaged rather than rejected: every character that is not a digit or the locale's
 * decimal separator is dropped, so `"€ 12,50"` parses to `12.5` under `de` and `"12abc34"` to
 * `1234`. Malformed input therefore yields a number, not a parse error.
 *
 * `null` means the input held nothing numeric at all (empty field, pure text) — the caller has to
 * tell it apart from `0`. Input consisting only of separator characters (`"."`, `","`) returns
 * `NaN`, so guard with `Number.isFinite` when the field accepts free text.
 */
export function useNumberParser(): (value: string, fractionDigits?: number) => number | null {
  const decimalSeparator = useDecimalSeparator();

  return useCallback(
    (value, fractionDigits) => {
      return parseNumber(value, decimalSeparator, fractionDigits);
    },
    [decimalSeparator],
  );
}
