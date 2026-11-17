/**
 * Imports
 */

import { skipToken } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createEntityQueries } from "./createEntityQueries";
import { entityQueryKeys } from "./entityQueryKeys";

/**
 * Mocks
 */

const mockFunctions = vi.hoisted(() => ({
  useQueryForStaticData: vi.fn(),
  useQueryForTableData: vi.fn(),
  useQueryForOneOffData: vi.fn(),
}));

vi.mock("./useQueryForStaticData", () => ({ useQueryForStaticData: mockFunctions.useQueryForStaticData }));
vi.mock("./useQueryForTableData", () => ({ useQueryForTableData: mockFunctions.useQueryForTableData }));
vi.mock("./useQueryForOneOffData", () => ({ useQueryForOneOffData: mockFunctions.useQueryForOneOffData }));

/**
 * Test Helpers
 */

interface BookListParams {
  page: number;
}

interface NewBookRequest {
  authorId?: string;
}

const functions = {
  byId: vi.fn((id: string) => Promise.resolve({ details: id })),
  preview: vi.fn((id: string) => Promise.resolve({ preview: id })),
  list: vi.fn((searchParams: BookListParams) => Promise.resolve({ list: searchParams })),
  template: vi.fn((request: NewBookRequest) => Promise.resolve({ template: request })),
};

const bookQueries = createEntityQueries(entityQueryKeys("books"), functions);

/**
 * The single options object the wrapper received.
 */
function receivedOptions(wrapper: ReturnType<typeof vi.fn>): { queryKey: unknown; queryFn: () => Promise<unknown> } {
  expect(wrapper).toHaveBeenCalledOnce();
  return wrapper.mock.calls[0]?.[0] as { queryKey: unknown; queryFn: () => Promise<unknown> };
}

/**
 * Tests
 */

describe("createEntityQueries", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useByIdQuery queries static data under byId(id) with the byId function", async () => {
    bookQueries.useByIdQuery("book-1");

    const options = receivedOptions(mockFunctions.useQueryForStaticData);
    expect(options.queryKey).toEqual(["books", "book-1"]);
    await expect(options.queryFn()).resolves.toEqual({ details: "book-1" });
    expect(functions.byId).toHaveBeenCalledWith("book-1");
  });

  it("usePreviewQuery queries static data under preview(id) with the preview function", async () => {
    bookQueries.usePreviewQuery("book-1");

    const options = receivedOptions(mockFunctions.useQueryForStaticData);
    expect(options.queryKey).toEqual(["books", "book-1", "preview"]);
    await expect(options.queryFn()).resolves.toEqual({ preview: "book-1" });
    expect(functions.preview).toHaveBeenCalledWith("book-1");
  });

  it("useByIdQuery without an id skips the request under the null key", () => {
    bookQueries.useByIdQuery(null);

    const options = receivedOptions(mockFunctions.useQueryForStaticData);
    expect(options.queryKey).toEqual(["books", null]);
    expect(options.queryFn).toBe(skipToken);
    expect(functions.byId).not.toHaveBeenCalled();
  });

  it("usePreviewQuery without an id skips the request under the null preview key", () => {
    bookQueries.usePreviewQuery(undefined);

    const options = receivedOptions(mockFunctions.useQueryForStaticData);
    expect(options.queryKey).toEqual(["books", null, "preview"]);
    expect(options.queryFn).toBe(skipToken);
    expect(functions.preview).not.toHaveBeenCalled();
  });

  it("useDataSliceQuery queries table data under list(searchParams) with the list function", async () => {
    bookQueries.useDataSliceQuery({ page: 2 });

    const options = receivedOptions(mockFunctions.useQueryForTableData);
    expect(options.queryKey).toEqual(["books", { page: 2 }]);
    await expect(options.queryFn()).resolves.toEqual({ list: { page: 2 } });
    expect(functions.list).toHaveBeenCalledWith({ page: 2 });
  });

  it("useNewQuery queries one-off data under template(request) with the template function", async () => {
    bookQueries.useNewQuery({ authorId: "author-1" });

    const options = receivedOptions(mockFunctions.useQueryForOneOffData);
    expect(options.queryKey).toEqual(["books", "new", { authorId: "author-1" }]);
    await expect(options.queryFn()).resolves.toEqual({ template: { authorId: "author-1" } });
    expect(functions.template).toHaveBeenCalledWith({ authorId: "author-1" });
  });

  it("useNewQuery without a request keys and sends an empty request", async () => {
    bookQueries.useNewQuery();

    const options = receivedOptions(mockFunctions.useQueryForOneOffData);
    expect(options.queryKey).toEqual(["books", "new", {}]);
    await options.queryFn();
    expect(functions.template).toHaveBeenCalledWith({});
  });

  it("useNewQuery requires the request when a template field is required", () => {
    const queries = createEntityQueries(entityQueryKeys("reviews"), {
      ...functions,
      template: (request: { bookId: string }) => Promise.resolve(request),
    });

    // @ts-expect-error the template request carries the required field `bookId`
    const callWithoutRequest = () => queries.useNewQuery();
    expect(callWithoutRequest).toBeTypeOf("function");
  });

  it("useNewQuery requires the request when the template function takes a request with a required field optionally", () => {
    const queries = createEntityQueries(entityQueryKeys("reviews"), {
      ...functions,
      template: (request?: { bookId: string }) => Promise.resolve(request),
    });

    // @ts-expect-error the template request carries the required field `bookId`
    const callWithUndefined = () => queries.useNewQuery(undefined);
    expect(callWithUndefined).toBeTypeOf("function");
  });

  it("useDataSliceQuery requires the search params when the list function takes them optionally", () => {
    const queries = createEntityQueries(entityQueryKeys("reviews"), {
      ...functions,
      list: (searchParams?: BookListParams) => Promise.resolve(searchParams),
    });

    // @ts-expect-error Orval declares the search params optional, the data slice needs them
    const callWithoutSearchParams = () => queries.useDataSliceQuery(undefined);
    expect(callWithoutSearchParams).toBeTypeOf("function");
  });
  it("without a preview function builds every query but the preview query", async () => {
    const { preview: _preview, ...functionsWithoutPreview } = functions;
    const queries = createEntityQueries(entityQueryKeys("books"), functionsWithoutPreview);

    expect(Object.keys(queries).toSorted()).toEqual(["useByIdQuery", "useDataSliceQuery", "useNewQuery"]);
    // @ts-expect-error the queries of an entity without a preview function have no preview query
    expect(queries.usePreviewQuery).toBeUndefined();

    queries.useByIdQuery("book-1");
    const options = receivedOptions(mockFunctions.useQueryForStaticData);
    expect(options.queryKey).toEqual(["books", "book-1"]);
    await expect(options.queryFn()).resolves.toEqual({ details: "book-1" });
  });
});
