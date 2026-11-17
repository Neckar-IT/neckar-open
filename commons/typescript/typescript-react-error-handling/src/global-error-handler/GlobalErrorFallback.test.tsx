/**
 * Imports
 */

import { act, render, screen, waitFor } from "@test/test-utils";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { makeAxiosError } from "./axiosErrorFixture";
import { type ErrorHandler, GlobalErrorFallback } from "./GlobalErrorFallback";
import { VersionInfoProvider } from "./VersionInfoContext";

/**
 * Tests
 */

describe("GlobalErrorFallback", () => {
  it("mounts a fresh error page for every new error, so page state such as the time of the error belongs to that error", async () => {
    let mounts = 0;
    function MountProbe() {
      const [mount] = useState(() => ++mounts);
      return <div>{`mount ${mount}`}</div>;
    }
    const errorHandlers: ErrorHandler[] = [() => <MountProbe />];

    const { rerender } = render(
      <GlobalErrorFallback error={new Error("first")} errorCaughtBy="window-error-listener" errorHandlers={errorHandlers} />,
    );
    const firstMount = (await screen.findByText(/^mount \d+$/)).textContent;

    rerender(<GlobalErrorFallback error={new Error("second")} errorCaughtBy="window-error-listener" errorHandlers={errorHandlers} />);

    await waitFor(() => {
      expect(screen.getByText(/^mount \d+$/).textContent).not.toBe(firstMount);
    });
  });

  it("keeps the page of the newer error when an older error resolves later", async () => {
    let releaseFirst: () => void = () => undefined;
    const firstReleased = new Promise<void>((resolve) => {
      releaseFirst = resolve;
    });
    // Awaits like an async error handler a product passes in.
    const slowForFirst: ErrorHandler = async (error) => {
      if (error instanceof Error && error.message === "first") {
        await firstReleased;
        return <div>page of first</div>;
      }
      return <div>page of second</div>;
    };

    const { rerender } = render(
      <GlobalErrorFallback error={new Error("first")} errorCaughtBy="window-error-listener" errorHandlers={[slowForFirst]} />,
    );
    rerender(<GlobalErrorFallback error={new Error("second")} errorCaughtBy="window-error-listener" errorHandlers={[slowForFirst]} />);
    expect(await screen.findByText("page of second")).toBeInTheDocument();

    // act flushes the late state update of the first error before the assertion looks.
    await act(async () => {
      releaseFirst();
      await firstReleased;
    });

    expect(screen.getByText("page of second")).toBeInTheDocument();
    expect(screen.queryByText("page of first")).toBeNull();
  });

  it("shows an error page for a value that is no Error", async () => {
    render(
      <VersionInfoProvider
        versionInfo={{
          imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
          imageDigestShort: "ae4608c7c590",
        }}
      >
        <GlobalErrorFallback error="plain string rejection" errorCaughtBy="window-error-listener" />
      </VersionInfoProvider>,
    );

    expect(await screen.findByText("plain string rejection")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Contact Support" })).toBeInTheDocument();
  });
});

describe("GlobalErrorFallback handler chain", () => {
  const versionInfo = {
    imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
    imageDigestShort: "ae4608c7c590",
  };

  function renderFallback(error: unknown) {
    return render(
      <VersionInfoProvider versionInfo={versionInfo}>
        <GlobalErrorFallback error={error} errorCaughtBy="global-error-boundary" />
      </VersionInfoProvider>,
    );
  }

  it.each([
    ["301", makeAxiosError({ status: 301 })],
    [
      "404 with payload",
      makeAxiosError({ status: 404, responseData: { type: "ExpectedError", errorCode: "BOOK-404", fallbackMessage: "Book not found" } }),
    ],
    ["409", makeAxiosError({ status: 409 })],
    [
      "500 with payload",
      makeAxiosError({ status: 500, responseData: { type: "UnexpectedError", exceptionType: "java.lang.IllegalStateException" } }),
    ],
    ["501", makeAxiosError({ status: 501 })],
  ])("renders `UnexpectedErrorPage` for %s", async (_name, error) => {
    renderFallback(error);

    expect(await screen.findByText("Something went wrong.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reload application" })).toBeInTheDocument();
  });

  it.each([502, 503, 504, undefined])("renders `ServiceUnavailablePage` without reload for HTTP %s", async (status) => {
    renderFallback(makeAxiosError({ status }));

    expect(await screen.findByRole("button", { name: "Back" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Reload application" })).not.toBeInTheDocument();
  });

  it("renders `PermissionDeniedPage` for 403 with AUTHZ-1", async () => {
    renderFallback(
      makeAxiosError({
        status: 403,
        responseData: {
          type: "ExpectedError",
          errorCode: "AUTHZ-1",
          fallbackMessage: "missing",
          attributes: { missingCapability: "patterns:finalize-order" },
        },
      }),
    );

    expect(await screen.findByText("HTTP 403")).toBeInTheDocument();
  });

  it("renders `PermissionDeniedPage` for 403 with AUTHZ-1 in the Blob body of a file request", async () => {
    const payload = {
      type: "ExpectedError",
      errorCode: "AUTHZ-1",
      fallbackMessage: "missing",
      attributes: { missingCapability: "patterns:finalize-order" },
    };
    renderFallback(makeAxiosError({ status: 403, responseData: new Blob([JSON.stringify(payload)], { type: "application/json" }) }));

    expect(await screen.findByText("HTTP 403")).toBeInTheDocument();
  });

  it("renders the version mismatch page for an errorCode starting with PVE- in the Blob body of a file request", async () => {
    const payload = { type: "ExpectedError", errorCode: "PVE-101", fallbackMessage: "old client" };
    renderFallback(makeAxiosError({ status: 400, responseData: new Blob([JSON.stringify(payload)], { type: "application/json" }) }));

    expect(await screen.findByText("New Version Available!")).toBeInTheDocument();
  });

  it("renders the version mismatch page for an errorCode starting with PVE-", async () => {
    renderFallback(
      makeAxiosError({ status: 400, responseData: { type: "ExpectedError", errorCode: "PVE-101", fallbackMessage: "old client" } }),
    );

    expect(await screen.findByText("New Version Available!")).toBeInTheDocument();
  });
});
