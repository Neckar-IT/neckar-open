/**
 * Imports
 */

import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NowProvider, useNow } from "./NowProvider";

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
    vi.resetAllMocks();
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
});
