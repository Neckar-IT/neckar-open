/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { renderHook } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { useQuantityFormatter, type QuantityFormatOptions } from "./useQuantityFormatter";

/**
 * The hook reads the language off the i18n context, which `renderHook` does not provide by itself.
 */
function renderFormatter(options: QuantityFormatOptions): (value: number) => string {
  return renderHook(() => useQuantityFormatter(options), { wrapper: I18nProvider }).result.current;
}

/**
 * Tests
 */

describe("useQuantityFormatter", () => {
  it("converts the stored base unit into the displayed one", () => {
    expect(renderFormatter({ conversionFactor: 0.01, fractionDigits: 2 })(10000)).toBe("100.00");
  });

  it("leaves the value untouched without a factor", () => {
    expect(renderFormatter({ fractionDigits: 2 })(10000)).toBe("10,000.00");
  });

  it("drops the grouping separator when grouping is off", () => {
    expect(renderFormatter({ useGrouping: false })(1998)).toBe("1998");
  });
});
