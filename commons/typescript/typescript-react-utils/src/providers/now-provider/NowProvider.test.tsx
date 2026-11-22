/**
 * Imports
 */

import { render, renderHook, screen } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NowProvider, nowMockedValue, useGetNow, useNow } from "./NowProvider";

/**
 * Component
 */

const Component = () => {
  const now = useNow();

  return <div data-testid="current-date">{now.toISOString()}</div>;
};

/**
 * Tests
 */

describe("now-provider", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it("should return the current date when no value is provided", () => {
    vi.setSystemTime(new Date("2024-01-02T03:04:05.000Z"));

    render(
      <NowProvider>
        <Component />
      </NowProvider>,
    );

    expect(screen.getByTestId("current-date").innerHTML).toBe("2024-01-02T03:04:05.000Z");
  });

  it("should return the date that is passed in as value", () => {
    render(
      <NowProvider value={new Date("2024-06-07T08:09:10.000Z")}>
        <Component />
      </NowProvider>,
    );

    expect(screen.getByTestId("current-date").innerHTML).toBe("2024-06-07T08:09:10.000Z");
  });

  it("throws without a NowProvider", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);

    expect(() => render(<Component />)).toThrow("useNow must be used within a NowProvider");
  });
});

describe("useGetNow", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it("returns a function that returns a copy of the passed value", () => {
    const { result } = renderHook(() => useGetNow(), {
      wrapper: ({ children }: PropsWithChildren) => <NowProvider value={nowMockedValue}>{children}</NowProvider>,
    });

    expect(result.current()).toEqual(nowMockedValue);
    expect(result.current()).not.toBe(nowMockedValue);
  });

  it("returns a function that returns the time of its call when no value is provided", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2024-01-02T03:04:05.000Z"));
    const { result } = renderHook(() => useGetNow(), { wrapper: NowProvider });

    vi.setSystemTime(new Date("2024-01-02T03:04:06.000Z"));

    expect(result.current().toISOString()).toBe("2024-01-02T03:04:06.000Z");
  });

  it("returns the same function between renders while the time of the value stays the same", () => {
    const { result, rerender } = renderHook(() => useGetNow(), {
      wrapper: ({ children }: PropsWithChildren) => <NowProvider value={new Date(nowMockedValue)}>{children}</NowProvider>,
    });
    const first = result.current;

    rerender();

    expect(result.current).toBe(first);
  });

  it("throws without a NowProvider", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);

    expect(() => renderHook(() => useGetNow())).toThrow("useGetNow must be used within a NowProvider");
  });
});
