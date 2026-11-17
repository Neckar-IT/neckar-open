import { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import { act, renderHook } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { usePaginationState } from "./usePaginationState";

function renderController() {
  return renderHook(() => {
    const { pagination, setPagination, withPaginationReset } = usePaginationState(25);
    const [freeText, setFreeTextValue] = useState<string | undefined>(undefined);
    const [sorting, setSortingValue] = useState<{ sortBy: string | undefined; sortOrder: SortOrder | undefined } | undefined>({
      sortBy: undefined,
      sortOrder: undefined,
    });
    return {
      pagination,
      setPagination,
      freeText,
      setFreeText: withPaginationReset(setFreeTextValue),
      sorting,
      setSorting: withPaginationReset(setSortingValue),
    };
  });
}

describe("usePaginationState", () => {
  it("starts at the page size handed to usePaginationState", () => {
    const { result } = renderController();

    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });

  it("resets the pageIndex when the free text changes", () => {
    const { result } = renderController();

    act(() => {
      result.current.setPagination({ pageIndex: 3, pageSize: 25 });
    });

    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });

    act(() => {
      result.current.setFreeText("Frank");
    });

    expect(result.current.freeText).toBe("Frank");
    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });

  it("resets the pageIndex when the sorting changes", () => {
    const { result } = renderController();

    act(() => {
      result.current.setPagination({ pageIndex: 3, pageSize: 25 });
    });

    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });

    act(() => {
      result.current.setSorting({ sortBy: "Title", sortOrder: SortOrder.Asc });
    });

    expect(result.current.sorting).toEqual({ sortBy: "Title", sortOrder: SortOrder.Asc });
    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });

  it("keeps a page size the table switched to", () => {
    const { result } = renderController();

    act(() => {
      result.current.setPagination({ pageIndex: 3, pageSize: 50 });
    });

    act(() => {
      result.current.setFreeText("Frank");
    });

    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 50 });
  });

  it("applies an updater handed to setPagination", () => {
    const { result } = renderController();

    act(() => {
      result.current.setPagination({ pageIndex: 3, pageSize: 25 });
    });

    act(() => {
      result.current.setPagination((old) => ({ ...old, pageIndex: old.pageIndex + 1 }));
    });

    expect(result.current.pagination).toEqual({ pageIndex: 4, pageSize: 25 });
  });

  it("restores the declared page size on setPagination(undefined)", () => {
    const { result } = renderController();

    act(() => {
      result.current.setPagination({ pageIndex: 3, pageSize: 50 });
    });

    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 50 });

    act(() => {
      result.current.setPagination(undefined);
    });

    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });

  it("gives a second mount its own state", () => {
    const first = renderController();

    act(() => {
      first.result.current.setPagination({ pageIndex: 3, pageSize: 25 });
    });

    const second = renderController();

    expect(second.result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
    expect(first.result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });
  });
});
