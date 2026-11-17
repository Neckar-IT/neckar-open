import { render, renderHook, screen } from "@test/test-utils";
import { AxiosError, AxiosHeaders } from "axios";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearUnknownOutcome, useSubmittingShell, useUnknownOutcome } from "../unknown-outcome/unknownOutcomeStore";
import { mutationFailedToastId, toast } from "./toast";
import { withActionFailedToast } from "./withActionFailedToast";

const toastErrorMock = vi.fn();
beforeEach(() => {
  toastErrorMock.mockReset();
  vi.spyOn(toast, "error").mockImplementation(toastErrorMock);
});
afterEach(() => {
  clearUnknownOutcome();
});

function errorWithStatus(status: number): AxiosError {
  const config = { headers: new AxiosHeaders(), url: "/api/patterns/publishers" };
  return new AxiosError("failed", AxiosError.ERR_BAD_RESPONSE, config, undefined, {
    data: undefined,
    status,
    statusText: "",
    headers: {},
    config,
  });
}

describe("withActionFailedToast", () => {
  it("resolves with the mutation result and shows no toast", async () => {
    await expect(withActionFailedToast("Speichern", Promise.resolve("good"))).resolves.toBe("good");

    expect(toastErrorMock).not.toHaveBeenCalled();
  });

  it("shows the failure toast under the mutation toast id and rejects with the same error", async () => {
    const failure = new Error("save failed");

    await expect(withActionFailedToast("Speichern", Promise.reject(failure))).rejects.toBe(failure);

    expect(toastErrorMock).toHaveBeenCalledOnce();
    expect(toastErrorMock.mock.calls[0]?.[1]).toEqual({ id: mutationFailedToastId });
    render(toastErrorMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText("Speichern failed.")).toBeInTheDocument();
  });

  it.each([401, 502, 503])("shows no toast of its own for HTTP %i and rejects with the same error", async (status) => {
    const failure = errorWithStatus(status);

    await expect(withActionFailedToast("Speichern", Promise.reject(failure))).rejects.toBe(failure);

    expect(toastErrorMock).not.toHaveBeenCalled();
  });

  it.each<[string, AxiosError]>([
    ["a 504", errorWithStatus(504)],
    ["a network error", new AxiosError("Network Error", AxiosError.ERR_NETWORK)],
  ])("shows the lasting toast of an unknown outcome for %s outside a form shell", async (_name, failure) => {
    await expect(withActionFailedToast("Speichern", Promise.reject(failure))).rejects.toBe(failure);

    expect(toastErrorMock).toHaveBeenCalledOnce();
    expect(toastErrorMock.mock.calls[0]?.[1]).toMatchObject({ id: mutationFailedToastId, duration: Infinity });
    render(toastErrorMock.mock.calls[0]?.[0] as ReactElement);
    expect(screen.getByText("‘Speichern’: outcome unclear")).toBeInTheDocument();
  });

  it("records a 504 for the submitting form shell instead of a toast", async () => {
    const { unmount } = renderHook(() => useSubmittingShell("shell-1", true));

    await expect(withActionFailedToast("Speichern", Promise.reject(errorWithStatus(504)))).rejects.toBeInstanceOf(AxiosError);

    expect(toastErrorMock).not.toHaveBeenCalled();
    expect(renderHook(() => useUnknownOutcome("shell-1")).result.current).toEqual({ shellId: "shell-1", action: "Speichern" });
    unmount();
  });
});
