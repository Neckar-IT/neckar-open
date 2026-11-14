import { act, renderHook, waitFor } from "@testing-library/react";
import { NuqsTestingAdapter, type UrlUpdateEvent } from "nuqs/adapters/testing";
import type { PropsWithChildren, ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { useEnumSearchParam } from "./useEnumSearchParam";

const TestStatus = {
  Pending: "Pending",
  Approved: "Approved",
} as const;

function makeWrapper(searchParams: string, onUrlUpdate?: (event: UrlUpdateEvent) => void): (props: PropsWithChildren) => ReactElement {
  return function Wrapper({ children }: PropsWithChildren): ReactElement {
    return (
      <NuqsTestingAdapter hasMemory searchParams={searchParams} onUrlUpdate={onUrlUpdate}>
        {children}
      </NuqsTestingAdapter>
    );
  };
}

describe("useEnumSearchParam", () => {
  it("reads an enum value from the URL", () => {
    const { result } = renderHook(() => useEnumSearchParam(TestStatus, "status"), {
      wrapper: makeWrapper("?status=Approved"),
    });

    expect(result.current[0]).toBe("Approved");
  });

  it("reads a value that is no enum member as undefined", () => {
    const { result } = renderHook(() => useEnumSearchParam(TestStatus, "status"), {
      wrapper: makeWrapper("?status=Unknown"),
    });

    expect(result.current[0]).toBeUndefined();
  });

  it("returns undefined without a default value when the URL carries no parameter", () => {
    const { result } = renderHook(() => useEnumSearchParam(TestStatus, "status"), {
      wrapper: makeWrapper(""),
    });

    expect(result.current[0]).toBeUndefined();
  });

  it("returns the default value when the URL carries no parameter, without writing it", () => {
    const onUrlUpdate = vi.fn();
    const { result } = renderHook(() => useEnumSearchParam(TestStatus, "status", TestStatus.Pending), {
      wrapper: makeWrapper("", onUrlUpdate),
    });

    expect(result.current[0]).toBe("Pending");
    expect(onUrlUpdate).not.toHaveBeenCalled();
  });

  it("writes the parameter to the URL when the state deviates from the default value", async () => {
    const onUrlUpdate = vi.fn();
    const { result } = renderHook(() => useEnumSearchParam(TestStatus, "status", TestStatus.Pending), {
      wrapper: makeWrapper("", onUrlUpdate),
    });

    await act(async () => {
      result.current[1](TestStatus.Approved);
    });

    expect(result.current[0]).toBe("Approved");
    await waitFor(() => expect(onUrlUpdate.mock.calls.at(-1)?.[0].searchParams.get("status")).toBe("Approved"));
  });

  it("removes the parameter from the URL when the state resets to the default value", async () => {
    const onUrlUpdate = vi.fn();
    const { result } = renderHook(() => useEnumSearchParam(TestStatus, "status", TestStatus.Pending), {
      wrapper: makeWrapper("?status=Approved", onUrlUpdate),
    });

    await act(async () => {
      result.current[1](TestStatus.Pending);
    });

    expect(result.current[0]).toBe("Pending");
    await waitFor(() => expect(onUrlUpdate.mock.calls.at(-1)?.[0].searchParams.has("status")).toBe(false));
  });
});
