import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";

/**
 * The text of a range in a filter button and in a chip: "3 – 9", "ab 3", "bis 9"; `undefined` without a bound.
 */
export function useRangeText(): (lower: string | undefined, upper: string | undefined) => string | undefined {
  const { t } = useEmbeddedTranslation();
  return (lower, upper) => {
    if (lower !== undefined && upper !== undefined) {
      return lower === upper ? lower : t({ key: "filterBar.rangeBetween", options: { from: lower, to: upper } });
    }
    if (lower !== undefined) {
      return t({ key: "filterBar.rangeFrom", options: { value: lower } });
    }
    if (upper !== undefined) {
      return t({ key: "filterBar.rangeTo", options: { value: upper } });
    }
    return undefined;
  };
}
