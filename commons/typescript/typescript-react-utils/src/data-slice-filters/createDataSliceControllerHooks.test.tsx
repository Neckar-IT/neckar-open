import { ArchiveFilter } from "@neckar-hub/typescript-utils/openapi-types";
import { act, renderHook } from "@testing-library/react";
import { NuqsTestingAdapter, type UrlUpdateEvent } from "nuqs/adapters/testing";
import type { PropsWithChildren, ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { createDataSliceControllerHooks } from "./createDataSliceControllerHooks";
import type { DataSliceParams } from "./DataSliceController";
import { archiveParam, integerParam, stringParam } from "./paramCodecs";

const BookSortCriteria = { Title: "Title", Year: "Year" } as const;
type BookSortCriteria = (typeof BookSortCriteria)[keyof typeof BookSortCriteria];

interface BookParams extends DataSliceParams<BookSortCriteria> {
  archiveFilter?: ArchiveFilter;
  freeText?: string;
  minYear?: number;
}

const bookDataSliceControllerHooks = createDataSliceControllerHooks<BookParams>(
  {
    archiveFilter: archiveParam(),
    freeText: stringParam(),
    minYear: integerParam(),
  },
  BookSortCriteria,
);

function makeWrapper(searchParams: string, onUrlUpdate: (event: UrlUpdateEvent) => void = () => {}) {
  return function Wrapper({ children }: PropsWithChildren): ReactElement {
    return (
      <NuqsTestingAdapter hasMemory searchParams={searchParams} onUrlUpdate={onUrlUpdate}>
        {children}
      </NuqsTestingAdapter>
    );
  };
}

describe("createDataSliceControllerHooks", () => {
  it("reads filter, pagination and sorting from the URL", () => {
    const { result } = renderHook(() => bookDataSliceControllerHooks.useFromUrl(), {
      wrapper: makeWrapper("?freeText=Dune&minYear=1965&archiveFilter=All&sortBy=Year&sortOrder=Desc&pageIndex=1"),
    });

    expect(result.current.params).toEqual({
      pageIndex: 1,
      pageSize: 10,
      sortBy: BookSortCriteria.Year,
      sortOrder: "Desc",
      archiveFilter: ArchiveFilter.All,
      freeText: "Dune",
      minYear: 1965,
    });
  });

  it("reads the prefixed URL keys only", () => {
    const { result } = renderHook(() => bookDataSliceControllerHooks.useFromUrl("books"), {
      wrapper: makeWrapper("?booksFreeText=Dune&freeText=ignored"),
    });

    expect(result.current.params.freeText).toBe("Dune");
  });

  it("starts the state controller at the codec defaults and changes it without a URL update", async () => {
    const onUrlUpdate = vi.fn<(event: UrlUpdateEvent) => void>();
    const { result } = renderHook(() => bookDataSliceControllerHooks.useFromState(), {
      wrapper: makeWrapper("?freeText=Dune", onUrlUpdate),
    });

    expect(result.current.params).toMatchObject({ pageIndex: 0, archiveFilter: ArchiveFilter.OnlyActive, freeText: undefined });

    await act(async () => {
      result.current.setParam("minYear", 1970);
    });

    expect(result.current.params.minYear).toBe(1970);
    expect(onUrlUpdate).not.toHaveBeenCalled();
  });
});
