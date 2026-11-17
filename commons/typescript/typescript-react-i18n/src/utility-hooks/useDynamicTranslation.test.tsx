/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import type { PropsWithChildren, ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { I18nProvider } from "../I18nProvider";
import { useDynamicTranslation } from "./useDynamicTranslation";

/**
 * Test Setup
 */

const languageResources = {
  de: {
    translation: {
      enums: {
        Visa: "Visa",
      },
    },
  },
};

function wrapper({ children }: PropsWithChildren): ReactElement {
  return (
    <I18nProvider languageResources={languageResources} options={{ lng: "de" }}>
      {children}
    </I18nProvider>
  );
}

/**
 * Tests
 */

describe("useDynamicTranslation", () => {
  beforeEach(() => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should resolve a known dynamic key and not warn", () => {
    const { result } = renderHook(() => useDynamicTranslation(), { wrapper });

    expect(result.current("enums.Visa")).toBe("Visa");
    expect(console.warn).not.toHaveBeenCalled();
  });

  it("should return the raw key and warn in DEV for an unknown dynamic key", () => {
    const { result } = renderHook(() => useDynamicTranslation(), { wrapper });

    expect(result.current("enums.Discover")).toBe("enums.Discover");
    expect(console.warn).toHaveBeenCalledWith('[i18n] Missing translation for dynamic key: "enums.Discover"');
  });
});
