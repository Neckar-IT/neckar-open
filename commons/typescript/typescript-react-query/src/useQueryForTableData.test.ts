/**
 * Imports
 */

import { restResponseGoodType } from "@neckar-hub/typescript-utils/rest";
import { describe, expect, it, vi } from "vitest";
import { paginationInfo } from "../mocks/mockPaginationInfo";
import { useQueryForTableData } from "./useQueryForTableData";

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

describe("useQueryForTableData", () => {
  it("should call the useQuery hook with the default settings", () => {
    const queryKey = ["some-query"];
    const queryFn = () => ({ type: restResponseGoodType, data: { id: 12 }, paginationInfo });

    useQueryForTableData({ queryKey, queryFn });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      refetchInterval: 30000,
      refetchOnMount: true,
      refetchOnWindowFocus: true,
      staleTime: 0,
    });
  });

  it("should call the useQuery hook with a customized refetchInterval", () => {
    const queryKey = ["some-query"];
    const queryFn = () => ({ type: restResponseGoodType, data: "some data", paginationInfo });
    const refetchInterval = 60000;

    useQueryForTableData({ queryKey, queryFn, refetchInterval });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      refetchInterval,
      refetchOnMount: true,
      refetchOnWindowFocus: true,
      staleTime: 0,
    });
  });
});
