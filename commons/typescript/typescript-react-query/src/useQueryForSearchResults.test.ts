/**
 * Imports
 */

import { restResponseGoodType } from "@neckar-hub/typescript-utils/rest";
import { keepPreviousData } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { paginationInfo } from "../mocks/mockPaginationInfo";
import { useQueryForSearchResults } from "./useQueryForSearchResults";

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

describe("useQueryForSearchResults", () => {
  it("should call the useQuery hook with the default settings", () => {
    const queryKey = ["customers", { anyText: "Hoff" }];
    const queryFn = () => ({ type: restResponseGoodType, data: [{ id: 12 }], paginationInfo });

    useQueryForSearchResults({ queryKey, queryFn });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      placeholderData: keepPreviousData,
      refetchOnMount: true,
      refetchOnWindowFocus: true,
      staleTime: 0,
    });
  });

  it("should call the useQuery hook with a customized staleTime", () => {
    const queryKey = ["customers", { anyText: "Hoff" }];
    const queryFn = () => ({ type: restResponseGoodType, data: [{ id: 12 }], paginationInfo });
    const staleTime = 60000;

    useQueryForSearchResults({ queryKey, queryFn, staleTime });

    expect(mockFunctions.useQuery).toHaveBeenCalledWith({
      queryKey,
      queryFn,
      placeholderData: keepPreviousData,
      refetchOnMount: true,
      refetchOnWindowFocus: true,
      staleTime,
    });
  });
});
