/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { ErrorHandler } from "./GlobalErrorFallback";
import { GlobalErrorHandler } from "./GlobalErrorHandler";

describe("GlobalErrorHandler", () => {
  it("should render the children if there is no error in the application", () => {
    render(
      <GlobalErrorHandler
        versionInfo={{
          imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
          imageDigestShort: "ae4608c7c590",
        }}
      >
        <div>children</div>
      </GlobalErrorHandler>,
    );
    expect(screen.getByText("children")).toBeInTheDocument();
  });

  it("should render the correct error page if there is an error during rendering", async () => {
    // suppress console.error output
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    const Children = () => {
      throw new Error("error from children");
    };
    render(
      <GlobalErrorHandler
        versionInfo={{
          imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
          imageDigestShort: "ae4608c7c590",
        }}
      >
        <Children />
      </GlobalErrorHandler>,
    );
    expect(await screen.findByText("Something went wrong.")).toBeInTheDocument();
    expect(screen.getByText("error from children")).toBeInTheDocument();
  });

  it("should render an error page if there is an error during a click handler", async () => {
    const user = userEvent.setup();
    render(
      <GlobalErrorHandler
        versionInfo={{
          imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
          imageDigestShort: "ae4608c7c590",
        }}
      >
        <button
          onClick={() => {
            throw new Error("error from onclick");
          }}
        >
          click me
        </button>
      </GlobalErrorHandler>,
    );
    await user.click(screen.getByText("click me"));
    expect(await screen.findByText("Something went wrong.")).toBeInTheDocument();
    expect(screen.getByText("error from onclick")).toBeInTheDocument();
  });

  it("should apply a custom error handler to an error the window listener catches", async () => {
    const user = userEvent.setup();
    const customErrorHandler: ErrorHandler = (error) =>
      error instanceof Error && error.message === "error from onclick" ? <div>custom error page</div> : null;
    render(
      <GlobalErrorHandler
        versionInfo={{
          imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
          imageDigestShort: "ae4608c7c590",
        }}
        errorHandlers={[customErrorHandler]}
      >
        <button
          onClick={() => {
            throw new Error("error from onclick");
          }}
        >
          click me
        </button>
      </GlobalErrorHandler>,
    );
    await user.click(screen.getByText("click me"));
    expect(await screen.findByText("custom error page")).toBeInTheDocument();
  });

  // TODO: test the handling of unhandled promise rejections.
  // In the browser the global error listener catches unhandled rejections (see the ladle stories), but not in vitest.
});
