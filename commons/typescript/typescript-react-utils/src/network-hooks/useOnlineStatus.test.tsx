/**
 * Imports
 */

import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { type HeartbeatRequest, useOnlineStatus } from "./useOnlineStatus";

/**
 * Setup
 */

beforeEach(() => {
  vi.clearAllMocks();
});

/**
 * Container
 */

function Container({ heartbeatRequests }: { heartbeatRequests: HeartbeatRequest | HeartbeatRequest[] }) {
  const { status } = useOnlineStatus(heartbeatRequests);
  return <div>{status}</div>;
}

/**
 * Tests
 */

describe("useOnlineStatus", () => {
  it("should return 'online' when the browser is online and all heartbeats are successful", async () => {
    const navigator = { onLine: true };
    vi.stubGlobal("navigator", navigator);

    const heartbeatRequests = {
      request: () =>
        new Promise((resolve) => {
          resolve("server available");
        }),
      message: "test",
    };

    render(<Container heartbeatRequests={heartbeatRequests} />);

    expect(await screen.findByText("online")).toBeInTheDocument();
  });

  it("should return 'offline' when the browser is offline", async () => {
    const navigator = { onLine: false };
    vi.stubGlobal("navigator", navigator);

    const heartbeatRequests = {
      request: () =>
        new Promise((resolve) => {
          resolve("server available");
        }),
      message: "test",
    };

    render(<Container heartbeatRequests={heartbeatRequests} />);

    expect(await screen.findByText("offline")).toBeInTheDocument();
  });

  it("should return 'server-not-available' when the browser is online but a heartbeat fails", async () => {
    const navigator = { onLine: true };
    vi.stubGlobal("navigator", navigator);

    const consoleError = vi.fn();
    vi.stubGlobal("console", { error: consoleError });

    const heartbeatRequests = {
      request: () =>
        new Promise<void>((_resolve, reject) => {
          reject(new Error("server not available"));
        }),
      message: "server not available",
    };

    render(<Container heartbeatRequests={heartbeatRequests} />);

    expect(await screen.findByText("server-not-available")).toBeInTheDocument();
    expect(consoleError).toHaveBeenCalledWith("server not available");
  });
});
