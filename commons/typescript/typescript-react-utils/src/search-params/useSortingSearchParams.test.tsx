import { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import { act, renderHook } from "@testing-library/react";
import { NuqsTestingAdapter } from "nuqs/adapters/testing";
import type { PropsWithChildren, ReactElement } from "react";
import { describe, expect, it } from "vitest";
import { usePaginationSearchParams } from "./usePaginationSearchParams";
import { useSortingSearchParams } from "./useSortingSearchParams";

const BookSortCriteria = {
  Title: "Title",
  Year: "Year",
} as const;

function makeWrapper(searchParams: string) {
  return function Wrapper({ children }: PropsWithChildren): ReactElement {
    return (
      <NuqsTestingAdapter hasMemory searchParams={searchParams}>
        {children}
      </NuqsTestingAdapter>
    );
  };
}

describe("useSortingSearchParams", () => {
  it("resets the pageIndex when the sorting changes", async () => {
    const { result } = renderHook(
      () => {
        const [sorting, setSorting] = useSortingSearchParams(BookSortCriteria);
        const [pagination] = usePaginationSearchParams();
        return { sorting, setSorting, pagination };
      },
      { wrapper: makeWrapper("?sortBy=Title&sortOrder=Asc&pageIndex=3&pageSize=25") },
    );

    expect(result.current.sorting).toEqual({ sortBy: "Title", sortOrder: SortOrder.Asc });
    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });

    await act(async () => {
      result.current.setSorting({ sortBy: BookSortCriteria.Year, sortOrder: SortOrder.Desc });
    });

    expect(result.current.sorting).toEqual({ sortBy: "Year", sortOrder: SortOrder.Desc });
    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 25 });
  });

  it("does not reset the pageIndex when setting the same sorting value", async () => {
    const { result } = renderHook(
      () => {
        const [sorting, setSorting] = useSortingSearchParams(BookSortCriteria);
        const [pagination] = usePaginationSearchParams();
        return { sorting, setSorting, pagination };
      },
      { wrapper: makeWrapper("?sortBy=Title&sortOrder=Asc&pageIndex=3&pageSize=25") },
    );

    expect(result.current.sorting).toEqual({ sortBy: "Title", sortOrder: SortOrder.Asc });
    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });

    await act(async () => {
      result.current.setSorting({ sortBy: BookSortCriteria.Title, sortOrder: SortOrder.Asc });
    });

    expect(result.current.sorting).toEqual({ sortBy: "Title", sortOrder: SortOrder.Asc });
    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 25 });
  });

  // See the same case in useSetterWithPaginationReset.test.tsx for why 20 and 10 are the values.
  it("keeps a pageSize that equals the helper's default and differs from the table's", async () => {
    const { result } = renderHook(
      () => {
        const [sorting, setSorting] = useSortingSearchParams(BookSortCriteria);
        const [pagination] = usePaginationSearchParams(20);
        return { sorting, setSorting, pagination };
      },
      { wrapper: makeWrapper("?sortBy=Title&sortOrder=Asc&pageIndex=3&pageSize=10") },
    );

    expect(result.current.pagination).toEqual({ pageIndex: 3, pageSize: 10 });

    await act(async () => {
      result.current.setSorting({ sortBy: BookSortCriteria.Year, sortOrder: SortOrder.Desc });
    });

    expect(result.current.pagination).toEqual({ pageIndex: 0, pageSize: 10 });
  });

  it("resets the prefixed pageIndex and leaves a foreign pageIndex untouched", async () => {
    const { result } = renderHook(
      () => {
        const [sorting, setSorting] = useSortingSearchParams(BookSortCriteria, "editions");
        const [editionsPagination] = usePaginationSearchParams(undefined, "editions");
        const [pagination] = usePaginationSearchParams();
        return { sorting, setSorting, editionsPagination, pagination };
      },
      { wrapper: makeWrapper("?editionsSortBy=Title&editionsSortOrder=Asc&editionsPageIndex=2&editionsPageSize=25&pageIndex=4") },
    );

    expect(result.current.editionsPagination).toEqual({ pageIndex: 2, pageSize: 25 });
    expect(result.current.pagination).toEqual({ pageIndex: 4, pageSize: 10 });

    await act(async () => {
      result.current.setSorting({ sortBy: BookSortCriteria.Year, sortOrder: SortOrder.Desc });
    });

    expect(result.current.sorting).toEqual({ sortBy: "Year", sortOrder: SortOrder.Desc });
    expect(result.current.editionsPagination).toEqual({ pageIndex: 0, pageSize: 25 });
    expect(result.current.pagination).toEqual({ pageIndex: 4, pageSize: 10 });
  });
});
