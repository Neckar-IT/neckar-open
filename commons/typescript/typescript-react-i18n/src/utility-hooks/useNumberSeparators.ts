import { useNumberFormatter } from "../formatter-hooks/useNumberFormatter";

export interface NumberSeparators {
  decimalSeparator: string;
  thousandSeparator: string;
}

/**
 * Reads the current locale's separators back out of a formatted sample number.
 *
 * `Intl` exposes no separator API, so `1234.5` is formatted with one fraction digit — the smallest
 * sample carrying both a grouping boundary and a decimal point — and whatever sits between `1`/`2`
 * and `4`/`5` is taken as the separator.
 *
 * Neither separator is ASCII in every locale: `fr` groups with U+202F (narrow no-break space),
 * `de-CH` with `'`. Compare against the returned value, never against a literal `"."` or `","`.
 *
 * Latin digits only. Under a locale whose default numbering system is not Latin (`ar-EG`, `fa`,
 * `bn`) the sample holds no `1`…`5` to anchor on and both fields come back as the whole formatted
 * number.
 */
export function useNumberSeparators(): NumberSeparators {
  const formatter = useNumberFormatter(1);

  const formattedValue = formatter(1234.5);

  const decimalSeparator = formattedValue.replace(/.*4(.*)5.*/, "$1");
  const thousandSeparator = formattedValue.replace(/.*1(.*)2.*/, "$1");

  return {
    decimalSeparator,
    thousandSeparator,
  };
}

export function useDecimalSeparator(): string {
  const { decimalSeparator } = useNumberSeparators();
  return decimalSeparator;
}
