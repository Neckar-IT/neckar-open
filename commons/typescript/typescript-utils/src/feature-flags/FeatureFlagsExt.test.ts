import { beforeEach, describe, expect, it, vi } from "vitest";
import { FeatureFlag } from "./FeatureFlag";
import { FeatureFlags } from "./FeatureFlags";
import { FeatureFlagsSupport } from "./FeatureFlagsSupport";
// Test feature flags - created inline since available flags come from the backend
const slowUiFlag = new FeatureFlag("slowUI");
const testingFlag = new FeatureFlag("testing");
/**
 * Tests for initializeFeatureFlagsFromUrlOrLocalStorage edge cases.
 *
 * Priority: URL parameter ALWAYS wins if it exists (even if empty).
 * LocalStorage is only used when URL parameter does NOT exist.
 *
 * Edge Cases Matrix:
 * | # | URL Parameter      | LocalStorage       | Expected Result              |
 * |---|--------------------|--------------------|------------------------------|
 * | 1 | not present        | not present        | empty (unchanged)            |
 * | 2 | not present        | "" (empty)         | empty (explicit)             |
 * | 3 | not present        | "slowUI"           | {slowUI} from LocalStorage   |
 * | 4 | "" (empty)         | not present        | empty (explicit from URL)    |
 * | 5 | "" (empty)         | "slowUI"           | empty (URL wins, ignores LS) |
 * | 6 | "slowUI"           | not present        | {slowUI} from URL            |
 * | 7 | "slowUI"           | "testing"          | {slowUI} from URL (priority) |
 * | 8 | "slowUI,testing"   | "other"            | {slowUI,testing} from URL    |
 */
// Mock window.location and localStorage
const mockLocation = {
  search: "",
  href: "http://localhost/",
};
const mockLocalStorage = new Map<string, string>();
const mockWindow = {
  location: mockLocation,
  localStorage: {
    getItem: (key: string) => mockLocalStorage.get(key) ?? null,
    setItem: (key: string, value: string) => mockLocalStorage.set(key, value),
    removeItem: (key: string) => mockLocalStorage.delete(key),
    clear: () => mockLocalStorage.clear(),
  },
  history: {
    replaceState: vi.fn(),
  },
};
// Apply mocks
vi.stubGlobal("window", mockWindow);
// Import after mocking
import {
  initializeFeatureFlagsFromUrlOrLocalStorage,
  loadFeatureFlagsFromLocalStorage,
  loadFeatureFlagsFromUrlOrLocalStorage,
  parseFeatureFlagsFromUrl,
} from "./FeatureFlagsExt";

describe("FeatureFlagsExt Edge Cases", () => {
  beforeEach(() => {
    // Reset state before each test
    mockLocation.search = "";
    mockLocation.href = "http://localhost/";
    mockLocalStorage.clear();
    FeatureFlagsSupport.flags = FeatureFlags.empty;
  });

  describe("parseFeatureFlagsFromUrl", () => {
    it("returns null when URL parameter is not present", () => {
      mockLocation.search = "";
      expect(parseFeatureFlagsFromUrl()).toBeNull();
    });

    it("returns empty FeatureFlags when URL parameter is empty string", () => {
      mockLocation.search = "?featureFlags=";
      const result = parseFeatureFlagsFromUrl();
      expect(result).not.toBeNull();
      expect(result?.isEmpty()).toBe(true);
    });

    it("returns FeatureFlags when URL parameter has value", () => {
      mockLocation.search = "?featureFlags=slowUI";
      const result = parseFeatureFlagsFromUrl();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
    });

    it("returns multiple flags from URL", () => {
      mockLocation.search = "?featureFlags=slowUI,testing";
      const result = parseFeatureFlagsFromUrl();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
      expect(result?.contains(testingFlag)).toBe(true);
    });
  });

  describe("loadFeatureFlagsFromLocalStorage", () => {
    it("returns null when localStorage key is not present", () => {
      expect(loadFeatureFlagsFromLocalStorage()).toBeNull();
    });

    it("returns empty FeatureFlags when localStorage value is empty string", () => {
      mockLocalStorage.set("featureFlags", "");
      const result = loadFeatureFlagsFromLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.isEmpty()).toBe(true);
    });

    it("returns FeatureFlags when localStorage has value", () => {
      mockLocalStorage.set("featureFlags", "slowUI");
      const result = loadFeatureFlagsFromLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
    });
  });

  describe("loadFeatureFlagsFromUrlOrLocalStorage", () => {
    // Case 1: URL not present, LocalStorage not present
    it("returns null when both URL and LocalStorage are not present", () => {
      mockLocation.search = "";
      // localStorage is empty
      expect(loadFeatureFlagsFromUrlOrLocalStorage()).toBeNull();
    });

    // Case 2: URL not present, LocalStorage empty
    it("returns empty FeatureFlags when URL not present and LocalStorage is empty", () => {
      mockLocation.search = "";
      mockLocalStorage.set("featureFlags", "");
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.isEmpty()).toBe(true);
    });

    // Case 3: URL not present, LocalStorage has value
    it("returns LocalStorage flags when URL not present", () => {
      mockLocation.search = "";
      mockLocalStorage.set("featureFlags", "slowUI");
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
    });

    // Case 4: URL empty, LocalStorage not present
    it("returns empty FeatureFlags when URL is empty (URL parameter exists)", () => {
      mockLocation.search = "?featureFlags=";
      // localStorage is empty
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.isEmpty()).toBe(true);
    });

    // Case 5: URL empty, LocalStorage has value - URL WINS (even if empty)
    it("returns empty FeatureFlags when URL is empty, ignoring LocalStorage", () => {
      mockLocation.search = "?featureFlags=";
      mockLocalStorage.set("featureFlags", "slowUI");
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      // URL parameter exists (even if empty) -> URL wins, LocalStorage ignored
      expect(result).not.toBeNull();
      expect(result?.isEmpty()).toBe(true);
      expect(result?.contains(slowUiFlag)).toBe(false);
    });

    // Case 6: URL has value, LocalStorage not present
    it("returns URL flags when LocalStorage not present", () => {
      mockLocation.search = "?featureFlags=slowUI";
      // localStorage is empty
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
    });

    // Case 7: URL has value, LocalStorage has different value - URL wins
    it("returns URL flags when both have values (URL priority)", () => {
      mockLocation.search = "?featureFlags=slowUI";
      mockLocalStorage.set("featureFlags", "testing");
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
      expect(result?.contains(testingFlag)).toBe(false);
    });

    // Case 8: URL has multiple flags
    it("returns all URL flags when URL has multiple values", () => {
      mockLocation.search = "?featureFlags=slowUI,testing";
      mockLocalStorage.set("featureFlags", "other");
      const result = loadFeatureFlagsFromUrlOrLocalStorage();
      expect(result).not.toBeNull();
      expect(result?.contains(slowUiFlag)).toBe(true);
      expect(result?.contains(testingFlag)).toBe(true);
    });
  });

  describe("initializeFeatureFlagsFromUrlOrLocalStorage", () => {
    it("does not change flags when no source has data", () => {
      const initialFlag = new FeatureFlag("initialFlag");
      FeatureFlagsSupport.flags = new FeatureFlags([initialFlag]);

      mockLocation.search = "";
      // localStorage is empty

      initializeFeatureFlagsFromUrlOrLocalStorage();

      // Flags should remain unchanged
      expect(FeatureFlagsSupport.flags.contains(initialFlag)).toBe(true);
    });

    it("sets flags from URL when available", () => {
      mockLocation.search = "?featureFlags=slowUI";

      initializeFeatureFlagsFromUrlOrLocalStorage();

      expect(FeatureFlagsSupport.flags.contains(slowUiFlag)).toBe(true);
    });

    it("overwrites localStorage when URL parameter exists", () => {
      mockLocation.search = "?featureFlags=slowUI";
      mockLocalStorage.set("featureFlags", "testing");

      initializeFeatureFlagsFromUrlOrLocalStorage();

      // URL flags should be active
      expect(FeatureFlagsSupport.flags.contains(slowUiFlag)).toBe(true);
      expect(FeatureFlagsSupport.flags.contains(testingFlag)).toBe(false);

      // localStorage should be updated with URL flags
      expect(mockLocalStorage.get("featureFlags")).toBe("slowUI");
    });

    it("overwrites localStorage with empty flags when URL is empty string", () => {
      mockLocation.search = "?featureFlags=";
      mockLocalStorage.set("featureFlags", "testing,slowUI");

      initializeFeatureFlagsFromUrlOrLocalStorage();

      // No flags should be active
      expect(FeatureFlagsSupport.flags.isEmpty()).toBe(true);

      // localStorage should be cleared (empty string)
      expect(mockLocalStorage.get("featureFlags")).toBe("");
    });

    it("sets flags from LocalStorage when URL not available", () => {
      mockLocation.search = "";
      mockLocalStorage.set("featureFlags", "testing");

      initializeFeatureFlagsFromUrlOrLocalStorage();

      expect(FeatureFlagsSupport.flags.contains(testingFlag)).toBe(true);
    });
  });
});
