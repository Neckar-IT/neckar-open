import { ExpectedApplicationErrorWithTypeType } from "@neckar-hub/typescript-utils/openapi-types";
import { render, renderHook, screen } from "@test/test-utils";
import { AxiosError, AxiosHeaders, CanceledError } from "axios";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { mutationFailedToastId, toast } from "../components/feedback/toast";
import { clearUnknownOutcome, useSubmittingShell, useUnknownOutcome } from "../components/feedback/unknown-outcome/unknownOutcomeStore";
import { createMutationErrorToastHandler } from "./createMutationErrorToastHandler";

const toastErrorMock = vi.fn();
beforeEach(() => {
  toastErrorMock.mockReset();
  vi.spyOn(toast, "error").mockImplementation(toastErrorMock);
});
afterEach(() => {
  clearUnknownOutcome();
});

function errorWithStatus(status: number, data?: unknown): AxiosError {
  const config = { headers: new AxiosHeaders(), url: "/api/patterns/publishers" };
  return new AxiosError("failed", AxiosError.ERR_BAD_RESPONSE, config, undefined, { data, status, statusText: "", headers: {}, config });
}

describe("createMutationErrorToastHandler", () => {
  it("reports the error and shows the error toast", () => {
    const report = vi.fn();
    const handler = createMutationErrorToastHandler({ report });
    const failure = new Error("save failed");

    handler(failure);

    expect(report).toHaveBeenCalledWith(failure);
    expect(toastErrorMock).toHaveBeenCalledTimes(1);
    expect(toastErrorMock.mock.calls[0]?.[1]).toEqual({ id: mutationFailedToastId });
  });

  it("shows the toast even without a reporter", () => {
    const handler = createMutationErrorToastHandler();

    handler(new Error("save failed"));

    expect(toastErrorMock).toHaveBeenCalledTimes(1);
  });

  it.each<[number, string]>([
    [502, "Not saved. The application cannot be reached right now."],
    [503, "Not saved. The application is unavailable right now."],
  ])("reports HTTP %i with the toast of a mutation the service did not run", (status, text) => {
    const report = vi.fn();
    const failure = errorWithStatus(status);

    createMutationErrorToastHandler({ report })(failure);

    expect(report).toHaveBeenCalledWith(failure);
    render(toastErrorMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it.each<[string, AxiosError]>([
    ["HTTP 504", errorWithStatus(504)],
    ["a network error", new AxiosError("Network Error", AxiosError.ERR_NETWORK)],
  ])("shows the lasting toast of an unknown outcome for %s", (_name, failure) => {
    createMutationErrorToastHandler()(failure);

    expect(toastErrorMock).toHaveBeenCalledOnce();
    expect(toastErrorMock.mock.calls[0]?.[1]).toMatchObject({ id: mutationFailedToastId, duration: Infinity });
  });

  it.each<[string, AxiosError]>([
    ["a same-origin 401", errorWithStatus(401)],
    [
      "a 403 with AUTHZ-1",
      errorWithStatus(403, {
        type: ExpectedApplicationErrorWithTypeType.ExpectedError,
        errorCode: "AUTHZ-1",
        fallbackMessage: "",
        attributes: { missingCapability: "patterns:finalize-order" },
      }),
    ],
    [
      "a PVE error code",
      errorWithStatus(400, { type: ExpectedApplicationErrorWithTypeType.ExpectedError, errorCode: "PVE-100", fallbackMessage: "" }),
    ],
    ["HTTP 500", errorWithStatus(500)],
  ])("shows the save-failed toast for %s", (_name, failure) => {
    createMutationErrorToastHandler()(failure);

    expect(toastErrorMock.mock.calls[0]?.[1]).toEqual({ id: mutationFailedToastId });
    render(toastErrorMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText("Saving failed.")).toBeInTheDocument();
  });

  it("records an unknown outcome for the submitting form shell instead of a toast", () => {
    const { unmount } = renderHook(() => useSubmittingShell("shell-1", true));

    createMutationErrorToastHandler()(errorWithStatus(504));

    expect(toastErrorMock).not.toHaveBeenCalled();
    expect(renderHook(() => useUnknownOutcome("shell-1")).result.current).toEqual({ shellId: "shell-1", action: undefined });
    unmount();
  });

  it("neither reports nor toasts a cancelled request", () => {
    const report = vi.fn();
    createMutationErrorToastHandler({ report })(new CanceledError());

    expect(report).not.toHaveBeenCalled();
    expect(toastErrorMock).not.toHaveBeenCalled();
  });
});
