import { act, renderHook } from "@testing-library/react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import type { PropsWithChildren, ReactElement } from "react";
import { describe, expect, it } from "vitest";
import { useFloatFilterSearchParam } from "./useFloatFilterSearchParam";
import { usePaginationSearchParams } from "./usePaginationSearchParams";

function makeWrapper(searchParams: string) {
  return function Wrapper({ children }: PropsWithChildren): ReactElement {
    return (
      <NuqsTestingAdapter hasMemory searchParams={searchParams}>
        {children}
      </NuqsTestingAdapter>
    );
  };
}

describe("useFloatFilterSearchParam", () => {
  it("reads a decimal from the URL", () => {
    const { result } = renderHook(() => useFloatFilterSearchParam<{ hourFactor: number }>("hourFactor"), {
      wrapper: makeWrapper("?hourFactor=1.5"),
    });

    expect(result.current[0]).toBe(1.5);
  });

  it("reads a value that is no number as undefined", () => {
    const { result } = renderHook(() => useFloatFilterSearchParam<{ hourFactor: number }>("hourFactor"), {
      wrapper: makeWrapper("?hourFactor=x"),
    });

    expect(result.current[0]).toBeUndefined();
  });

  it("resets the pageIndex when the filter changes", async () => {
    const { result } = renderHook(
      () => {
        const [hourFactor, setHourFactor] = useFloatFilterSearchParam<{ hourFactor: number }>("hourFactor");
        const [pagination] = usePaginationSearchParams();
        return { hourFactor, setHourFactor, pagination };
      },
      { wrapper: makeWrapper("?hourFactor=1.5&pageIndex=3&pageSize=25") },
    );

    await act(async () => {
      result.current.setHourFactor(2.25);
    });

    expect(result.current.hourFactor).toBe(2.25);
    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });
});
