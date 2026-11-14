import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vitest } from "vitest";
import { testModeWindowProperty } from "../test-mode/isTestMode";
import { useCountdown } from "./useCountdown";

beforeEach(() => {
  vitest.useFakeTimers();
});

afterEach(() => {
  vitest.useRealTimers();
  window[testModeWindowProperty] = undefined;
});

describe("useCountdown()", () => {
  it("should return callable functions", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 500, isIncrement: false }));

    expect(result.current[0]).toBe(60);
    expect(typeof result.current[1].startCountdown).toBe("function");
    expect(typeof result.current[1].stopCountdown).toBe("function");
    expect(typeof result.current[1].resetCountdown).toBe("function");
  });

  it("should increment count", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 500, isIncrement: true }));

    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(1000);
    });

    expect(result.current[0]).toBe(62);
  });

  it("should decrement count", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 500 }));

    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(1000);
    });

    expect(result.current[0]).toBe(58);
  });

  it("should accept countStart", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 30 }));

    expect(result.current[0]).toBe(30);
    expect(typeof result.current[1].startCountdown).toBe("function");
    expect(typeof result.current[1].stopCountdown).toBe("function");
    expect(typeof result.current[1].resetCountdown).toBe("function");
  });

  it("should accept intervalMs", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 500 }));

    expect(result.current[0]).toBe(60);
    expect(typeof result.current[1].startCountdown).toBe("function");
    expect(typeof result.current[1].stopCountdown).toBe("function");
    expect(typeof result.current[1].resetCountdown).toBe("function");

    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(500);
    });

    expect(result.current[0]).toBe(59);
  });

  it("should automatically start running if initiallyRunning is true", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 1000, initiallyRunning: true }));

    expect(result.current[0]).toBe(60);

    act(() => {
      vitest.advanceTimersByTime(1000);
    });

    expect(result.current[0]).toBe(59);
  });

  it("should not tick with initiallyRunning in test mode", () => {
    window[testModeWindowProperty] = true;
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 1000, initiallyRunning: true }));

    act(() => {
      vitest.advanceTimersByTime(3000);
    });

    expect(result.current[0]).toBe(60);
  });

  it("should stop at countStop (default: 0)", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 1000 }));

    expect(result.current[0]).toBe(60);
    expect(typeof result.current[1].startCountdown).toBe("function");
    expect(typeof result.current[1].stopCountdown).toBe("function");
    expect(typeof result.current[1].resetCountdown).toBe("function");

    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(60 * 1000);
    });

    expect(result.current[0]).toBe(0);

    act(() => {
      vitest.advanceTimersByTime(1000);
    });

    expect(result.current[0]).toBe(0);
  });

  it("should stop at custom countStop", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 1000, countStop: 30 }));

    expect(result.current[0]).toBe(60);
    expect(typeof result.current[1].startCountdown).toBe("function");
    expect(typeof result.current[1].stopCountdown).toBe("function");
    expect(typeof result.current[1].resetCountdown).toBe("function");

    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(30 * 1000);
    });

    expect(result.current[0]).toBe(30);

    act(() => {
      vitest.advanceTimersByTime(1000);
    });

    expect(result.current[0]).toBe(30);
  });

  it("should stop countdown", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 1000 }));

    expect(result.current[0]).toBe(60);
    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(2000);
    });

    expect(result.current[0]).toBe(58);
    act(result.current[1].stopCountdown);
    act(() => {
      vitest.advanceTimersByTime(3000);
    });
    expect(result.current[0]).toBe(58);
  });

  it("should stop reversed countdown", () => {
    const { result } = renderHook(() =>
      useCountdown({
        countStart: 10,
        intervalMs: 1000,
        countStop: 20,
        isIncrement: true,
      }),
    );

    expect(result.current[0]).toBe(10);
    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(2 * 1000);
    });

    expect(result.current[0]).toBe(12);

    act(() => {
      vitest.advanceTimersByTime(8 * 1000);
    });
    expect(result.current[0]).toBe(20);

    act(() => {
      vitest.advanceTimersByTime(3 * 1000);
    });

    expect(result.current[0]).toBe(20);
  });

  it("should reset count", () => {
    const { result } = renderHook(() => useCountdown({ countStart: 60, intervalMs: 1000 }));

    act(result.current[1].startCountdown);
    act(() => {
      vitest.advanceTimersByTime(1000);
    });
    act(result.current[1].stopCountdown);
    expect(result.current[0]).toBeLessThan(60);

    act(result.current[1].resetCountdown);
    expect(result.current[0]).toBe(60);
  });
});
