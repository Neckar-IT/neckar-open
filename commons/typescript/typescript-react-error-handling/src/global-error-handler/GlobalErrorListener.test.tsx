/**
 * Imports
 */

import { act, render, screen } from "@test/test-utils";
import { CanceledError } from "axios";
import { describe, expect, it, vi } from "vitest";
import { makeAxiosError } from "./axiosErrorFixture";
import { GlobalErrorListener } from "./GlobalErrorListener";

/**
 * Fixtures
 */

function dispatchUnhandledRejection(reason: unknown): void {
  const event = Object.assign(new Event("unhandledrejection"), { reason, promise: Promise.resolve() });
  act(() => {
    window.dispatchEvent(event);
  });
}

/**
 * Tests
 */

describe("GlobalErrorListener", () => {
  it.each<[string, unknown]>([
    ["HTTP 502", makeAxiosError({ status: 502 })],
    ["HTTP 503", makeAxiosError({ status: 503 })],
    ["HTTP 504", makeAxiosError({ status: 504 })],
    ["a network error", makeAxiosError()],
    ["a same-origin HTTP 401", makeAxiosError({ status: 401 })],
    ["a cancelled request", new CanceledError()],
  ])("leaves the children mounted for an unhandled rejection with %s", (_name, reason) => {
    vi.spyOn(console, "warn").mockImplementation(() => undefined);
    render(
      <GlobalErrorListener errorHandlers={[() => <div>error page</div>]}>
        <div>form</div>
      </GlobalErrorListener>,
    );

    dispatchUnhandledRejection(reason);

    expect(screen.getByText("form")).toBeInTheDocument();
    expect(screen.queryByText("error page")).not.toBeInTheDocument();
  });

  it("replaces the children for an unhandled rejection with HTTP 500", async () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    render(
      <GlobalErrorListener errorHandlers={[() => <div>error page</div>]}>
        <div>form</div>
      </GlobalErrorListener>,
    );

    dispatchUnhandledRejection(makeAxiosError({ status: 500 }));

    expect(await screen.findByText("error page")).toBeInTheDocument();
  });

  it("stops listening once it unmounts", () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const { unmount } = render(
      <GlobalErrorListener errorHandlers={[() => <div>error page</div>]}>
        <div>form</div>
      </GlobalErrorListener>,
    );
    unmount();

    dispatchUnhandledRejection(makeAxiosError({ status: 500 }));

    expect(consoleError).not.toHaveBeenCalledWith("[GlobalErrorListener] unhandled promise rejection:", expect.anything());
  });
});
