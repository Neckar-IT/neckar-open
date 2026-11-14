import { QueryStatus, type QueryResult } from "@neckar-hub/typescript-react-query";
import type { RestResponse } from "@neckar-hub/typescript-utils/rest";
import { describe, expect, it } from "vitest";
import { resolvedById } from "./resolvedById";

function pending(): QueryResult<RestResponse<string>> {
  return { status: QueryStatus.Pending, response: undefined, meta: undefined, queryKey: ["authors", null] };
}

function success(response: RestResponse<string>): QueryResult<RestResponse<string>> {
  return { status: QueryStatus.Success, response, meta: undefined, queryKey: ["authors", "author-1"] };
}

describe("resolvedById", () => {
  it("returns null for an absent id", () => {
    expect(resolvedById(null, pending())).toBeNull();
    expect(resolvedById(undefined, pending())).toBeNull();
  });

  it("returns undefined while the query is pending", () => {
    expect(resolvedById("author-1", pending())).toBeUndefined();
  });

  it("returns the data of a good response", () => {
    expect(resolvedById("author-1", success({ type: "good", data: "Ada Lovelace" }))).toBe("Ada Lovelace");
  });

  it("returns null for a bad response", () => {
    expect(resolvedById("author-1", success({ type: "bad/NotFound" }))).toBeNull();
  });
});
