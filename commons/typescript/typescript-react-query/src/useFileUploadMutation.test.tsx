/**
 * Imports
 */

import { QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { CanceledError, type AxiosProgressEvent } from "axios";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";
import { createQueryClient } from "./createQueryClient";
import { type FileUploadFunction, useFileUploadMutation } from "./useFileUploadMutation";

/**
 * Test Helpers
 */

const file = new File(["content"], "terms.pdf", { type: "application/pdf" });

function createWrapper() {
  const queryClient = createQueryClient();
  // The global default `throwOnError: true` would rethrow the failed mutation into the render.
  queryClient.setDefaultOptions({ mutations: { throwOnError: false } });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

/**
 * An upload that settles when the test calls `resolve` or `reject`, or rejects when the signal is aborted.
 */
function createControlledUpload() {
  let onUploadProgress: (event: AxiosProgressEvent) => void = () => {};
  let resolve: (value: string) => void = () => {};
  let reject: (error: Error) => void = () => {};

  const upload = vi.fn<FileUploadFunction<string>>((_file, config) => {
    onUploadProgress = config.onUploadProgress;
    return new Promise<string>((promiseResolve, promiseReject) => {
      resolve = promiseResolve;
      reject = promiseReject;
      config.signal.addEventListener("abort", () => promiseReject(new CanceledError("canceled")));
    });
  });

  return {
    upload,
    reportProgress: (progress: number | undefined) => onUploadProgress({ loaded: 0, bytes: 0, progress } as AxiosProgressEvent),
    resolve: (value: string) => resolve(value),
    reject: (error: Error) => reject(error),
  };
}

/**
 * Tests
 */

describe("useFileUploadMutation", () => {
  it("reports progress in whole percent, switches to processing at 100% and resets it after success", async () => {
    const controlled = createControlledUpload();
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useFileUploadMutation({ upload: controlled.upload, onSuccess }), { wrapper: createWrapper() });

    expect(result.current.progress).toEqual({ kind: "idle" });

    act(() => result.current.mutate(file));
    await waitFor(() => expect(result.current.isPending).toBe(true));
    expect(result.current.progress).toEqual({ kind: "indeterminate" });

    act(() => controlled.reportProgress(0.4567));
    expect(result.current.progress).toEqual({ kind: "uploading", fraction: 0.45 });

    act(() => controlled.reportProgress(1));
    expect(result.current.progress).toEqual({ kind: "processing" });

    await act(async () => controlled.resolve("done"));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.progress).toEqual({ kind: "idle" });
    expect(result.current.data).toBe("done");
    expect(onSuccess).toHaveBeenCalledWith("done", file);
  });

  it("stays indeterminate while the event carries no progress", async () => {
    const controlled = createControlledUpload();
    const { result } = renderHook(() => useFileUploadMutation({ upload: controlled.upload }), { wrapper: createWrapper() });

    act(() => result.current.mutate(file));
    await waitFor(() => expect(result.current.isPending).toBe(true));

    act(() => controlled.reportProgress(undefined));
    expect(result.current.progress).toEqual({ kind: "indeterminate" });

    await act(async () => controlled.resolve("done"));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it("resets progress after an error and does not retry", async () => {
    const controlled = createControlledUpload();
    const { result } = renderHook(() => useFileUploadMutation({ upload: controlled.upload }), { wrapper: createWrapper() });

    act(() => result.current.mutate(file));
    await waitFor(() => expect(result.current.isPending).toBe(true));
    act(() => controlled.reportProgress(0.5));
    expect(result.current.progress).toEqual({ kind: "uploading", fraction: 0.5 });

    await act(async () => controlled.reject(new Error("boom")));
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.progress).toEqual({ kind: "idle" });
    expect(controlled.upload).toHaveBeenCalledTimes(1);
  });

  it("ends in isError and resets progress when aborted", async () => {
    const controlled = createControlledUpload();
    const { result } = renderHook(() => useFileUploadMutation({ upload: controlled.upload }), { wrapper: createWrapper() });

    act(() => result.current.mutate(file));
    await waitFor(() => expect(result.current.isPending).toBe(true));
    act(() => controlled.reportProgress(0.3));

    act(() => result.current.abort());
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.progress).toEqual({ kind: "idle" });
  });
});
