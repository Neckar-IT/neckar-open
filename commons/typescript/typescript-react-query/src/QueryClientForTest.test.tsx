import { useQueryClient } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { QueryClientForTest } from "./QueryClientForTest";

describe("QueryClientForTest", () => {
  it("provides a query client of its own to every mount", () => {
    const first = renderHook(() => useQueryClient(), { wrapper: QueryClientForTest });
    const second = renderHook(() => useQueryClient(), { wrapper: QueryClientForTest });

    expect(first.result.current).not.toBe(second.result.current);
  });

  it("keeps the query client of a mount across rerenders", () => {
    const { result, rerender } = renderHook(() => useQueryClient(), { wrapper: QueryClientForTest });
    const before = result.current;

    rerender();

    expect(result.current).toBe(before);
  });

  it("starts no garbage-collection timer", () => {
    const { result } = renderHook(() => useQueryClient(), { wrapper: QueryClientForTest });

    expect(result.current.getDefaultOptions().queries?.gcTime).toBe(Infinity);
    expect(result.current.getDefaultOptions().mutations?.gcTime).toBe(Infinity);
  });
});
