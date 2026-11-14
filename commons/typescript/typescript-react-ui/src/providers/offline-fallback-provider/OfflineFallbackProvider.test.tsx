/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { OfflineFallbackProvider } from "./OfflineFallbackProvider";

/**
 * Tests
 */

describe("OfflineFallbackProvider", () => {
  const setOnlineStatus = (isOnline: boolean) => {
    Object.defineProperty(window.navigator, "onLine", {
      value: isOnline,
      configurable: true,
    });
  };

  it("should render the children when the browser is online", () => {
    // Simulate being online
    setOnlineStatus(true);

    render(
      <OfflineFallbackProvider>
        <div data-testid="children">Content</div>
      </OfflineFallbackProvider>,
    );

    expect(screen.getByTestId("children")).toBeInTheDocument();
  });

  it("should render the fallback when the browser is offline", () => {
    // Simulate being offline
    setOnlineStatus(false);

    render(
      <OfflineFallbackProvider>
        <div data-testid="children">Content</div>
      </OfflineFallbackProvider>,
    );

    expect(screen.queryByTestId("children")).not.toBeInTheDocument();
    expect(screen.getByText("You are offline!"));
  });

  it("should update when switching from offline to online", async () => {
    // Simulate being offline
    setOnlineStatus(false);

    render(
      <OfflineFallbackProvider>
        <div data-testid="children">Content</div>
      </OfflineFallbackProvider>,
    );

    expect(screen.queryByTestId("children")).not.toBeInTheDocument();
    expect(screen.getByText("You are offline!"));

    // Simulate coming online
    setOnlineStatus(true);
    window.dispatchEvent(new Event("online"));

    expect(await screen.findByTestId("children")).toBeInTheDocument();
  });

  it("should update when switching from online to offline", async () => {
    // Simulate being offline
    setOnlineStatus(true);

    render(
      <OfflineFallbackProvider>
        <div data-testid="children">Content</div>
      </OfflineFallbackProvider>,
    );

    expect(screen.getByTestId("children")).toBeInTheDocument();

    // Simulate coming online
    setOnlineStatus(false);
    window.dispatchEvent(new Event("offline"));

    expect(await screen.findByText("You are offline!"));
  });
});
