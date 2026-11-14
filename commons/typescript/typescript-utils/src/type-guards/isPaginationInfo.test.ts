/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import type { PaginationInfo } from "../../build/generated/open-api/openapi-types.schemas";
import { isPaginationInfo } from "./isPaginationInfo";

/**
 * Tests
 */

describe("isPaginationInfo", () => {
  it("should return true for valid PaginationInfo objects", () => {
    const validPaginationInfo: PaginationInfo = {
      pageIndex: 1,
      pageSize: 10,
      pageCount: 5,
      filteredElementsCount: 50,
      totalElementsCount: 100,
    };

    expect(isPaginationInfo(validPaginationInfo)).toBe(true);
  });

  it("should return false for invalid PaginationInfo objects", () => {
    const invalidPaginationInfo = {
      pageIndex: "1",
      pageSize: 10,
      pageCount: 5,
      filteredElementsCount: 50,
      totalElementsCount: 100,
    };

    expect(isPaginationInfo(invalidPaginationInfo)).toBe(false);
  });

  it("should return false for non-object types", () => {
    expect(isPaginationInfo(null)).toBe(false);
    expect(isPaginationInfo(undefined)).toBe(false);
    expect(isPaginationInfo(42)).toBe(false);
    expect(isPaginationInfo("string")).toBe(false);
    expect(isPaginationInfo([])).toBe(false);
  });
});
