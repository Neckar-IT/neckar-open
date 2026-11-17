/**
 * Imports
 */

import { restResponseGoodType } from "@neckar-hub/typescript-utils/rest";
import { describe, expect, it, vi } from "vitest";
import { paginationInfo } from "../mocks/mockPaginationInfo";
import { useQueryForDynamicData } from "./useQueryForDynamicData";

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

describe("useQueryForDynamicData", () => {
  it("should call the useQuery hook with the default settings", () => {
    const queryKey = ["some-query"];
    const queryFn = () => ({ type: restResponseGoodType, data: "some data", paginationInfo });

    useQueryForDynamicData({ queryKey, queryFn });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      refetchInterval: 1000,
      refetchOnMount: false,
      refetchOnWindowFocus: false,
      staleTime: 0,
      retry: false,
    });
  });

  it("should call the useQueryWithCustomNamingScheme hook with a custom refetch interval", () => {
    const queryKey = ["some-query"];
    const queryFn = () => ({ type: restResponseGoodType, data: "some data", paginationInfo });
    const refetchInterval = 5000;

    useQueryForDynamicData({ queryKey, queryFn, refetchInterval });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      refetchInterval,
      refetchOnMount: false,
      refetchOnWindowFocus: false,
      staleTime: 0,
      retry: false,
    });
  });
});
