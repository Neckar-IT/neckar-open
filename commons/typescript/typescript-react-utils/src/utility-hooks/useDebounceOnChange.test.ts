/**
 * Imports
 */

import { renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useDebounceOnChange } from "./useDebounceOnChange";

/**
 * Tests
 */

describe("useDebounceOnChange", () => {
  it("should return the initial value", () => {
    const handleCallback = vi.fn();

    const { result } = renderHook(() => useDebounceOnChange({ initialValue: "initial", debouncedCallback: handleCallback }));

    expect(result.current.inputValue).toBe("initial");
  });

  it("should call the dbounced callback function after the delay", async () => {
    const handleCallback = vi.fn();

    const { result } = renderHook(() => useDebounceOnChange({ initialValue: "initial", debouncedCallback: handleCallback, delay: 50 }));

    result.current.handleChange("changed");

    expect(handleCallback).not.toHaveBeenCalled();
    await vi.waitFor(() => expect(handleCallback).toHaveBeenCalled(), { timeout: 500, interval: 10 });
    expect(handleCallback).toHaveBeenCalledWith("changed");
  });
});
