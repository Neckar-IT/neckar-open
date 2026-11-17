import { act, renderHook } from "@testing-library/react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import type { PropsWithChildren, ReactElement } from "react";
import { describe, expect, it } from "vitest";
import { usePaginationSearchParams } from "./usePaginationSearchParams";
import { useStringFilterSearchParam } from "./useStringFilterSearchParam";

function makeWrapper(searchParams: string) {
  return function Wrapper({ children }: PropsWithChildren): ReactElement {
    return (
      <NuqsTestingAdapter hasMemory searchParams={searchParams}>
        {children}
      </NuqsTestingAdapter>
    );
  };
}

describe("useSetterWithPaginationReset", () => {
  it("resets the pageIndex when the filter changes", async () => {
    const { result } = renderHook(
      () => {
        const [name, setName] = useStringFilterSearchParam<{ name: string }>("name");
        const [pagination] = usePaginationSearchParams();
        return { name, setName, pagination };
      },
      { wrapper: makeWrapper("?name=old&pageIndex=3&pageSize=25") },
    );

    expect(result.current.name).toBe("old");
    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });

    await act(async () => {
      result.current.setName("new");
    });

    expect(result.current.name).toBe("new");
    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });

  // The URL carries the helper's own default page size, 10, and the table declares 20. A reset writing
  // the whole object would hand nuqs a 10 it drops as the parser default, and the table would show 20.
  it("keeps a pageSize that equals the helper's default and differs from the table's", async () => {
    const { result } = renderHook(
      () => {
        const [name, setName] = useStringFilterSearchParam<{ name: string }>("name");
        const [pagination] = usePaginationSearchParams(20);
        return { name, setName, pagination };
      },
      { wrapper: makeWrapper("?name=old&pageIndex=3&pageSize=10") },
    );

    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 10 });

    await act(async () => {
      result.current.setName("new");
    });

    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 10 });
  });

  it("resets the prefixed pageIndex and leaves a foreign pageIndex untouched", async () => {
    const { result } = renderHook(
      () => {
        const [name, setName] = useStringFilterSearchParam<{ name: string }>("name", "editions");
        const [editionsPagination] = usePaginationSearchParams(undefined, "editions");
        const [pagination] = usePaginationSearchParams();
        return { name, setName, editionsPagination, pagination };
      },
      { wrapper: makeWrapper("?editionsName=old&editionsPageIndex=2&editionsPageSize=25&pageIndex=4") },
    );

    expect(result.current.editionsPagination).toEqual({ pageIndex: 2, pageSize: 25 });
    expect(result.current.pagination).toEqual({ pageIndex: 4, pageSize: 10 });

    await act(async () => {
      result.current.setName("new");
    });

    expect(result.current.name).toBe("new");
    expect(result.current.editionsPagination).toEqual({ pageIndex: 0, pageSize: 25 });
    expect(result.current.pagination).toEqual({ pageIndex: 4, pageSize: 10 });
  });
});
