/**
 * Imports
 */

import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useSetAsState } from "./useSetAsState";

/**
 * Tests
 */

describe("useSetAsState()", () => {
  it("should initialize the set with the given values", () => {
    const { result } = renderHook(() => useSetAsState([1, 2, 3]));

    expect(result.current.has(1)).toBe(true);
    expect(result.current.has(2)).toBe(true);
    expect(result.current.has(3)).toBe(true);
  });

  it("should add a value to the set", () => {
    const { result } = renderHook(() => useSetAsState([1, 2]));

    act(() => {
      result.current.add(4);
    });

    expect(result.current.has(1)).toBe(true);
    expect(result.current.has(2)).toBe(true);
    expect(result.current.has(4)).toBe(true);
  });

  it("should delete a value from the set", () => {
    const { result } = renderHook(() => useSetAsState([1, 2]));

    act(() => {
      result.current.delete(1);
    });

    expect(result.current.has(1)).toBe(false);
    expect(result.current.has(2)).toBe(true);
  });

  it("should clear the set", () => {
    const { result } = renderHook(() => useSetAsState([1, 2]));

    act(() => {
      result.current.clear();
    });

    expect(result.current.has(1)).toBe(false);
    expect(result.current.has(2)).toBe(false);
  });
});
