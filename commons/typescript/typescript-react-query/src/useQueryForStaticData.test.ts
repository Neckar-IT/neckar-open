/**
 * Imports
 */

import { restResponseGoodType } from "@neckar-hub/typescript-utils/rest";
import { describe, expect, it, vi } from "vitest";
import { paginationInfo } from "../mocks/mockPaginationInfo";
import { useQueryForStaticData } from "./useQueryForStaticData";

/**
 * Mocks
 */

const mockFunctions = vi.hoisted(() => ({
  useQuery: vi.fn(),
}));

vi.mock("./useQuery", () => ({
  useQuery: mockFunctions.useQuery,
}));

/**
 * Tests
 */

describe("useQueryForStaticData", () => {
  it("should call the useQueryWith hook with the default settings", () => {
    const queryKey = ["some-query"];
    const queryFn = () => ({ type: restResponseGoodType, data: "some data", paginationInfo });

    useQueryForStaticData({ queryKey, queryFn });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      refetchOnMount: true,
      refetchOnWindowFocus: true,
    });
  });
});
