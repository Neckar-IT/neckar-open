import { describe, expect, it } from "vitest";
import { entityQueryKeys } from "./entityQueryKeys";

describe("entityQueryKeys", () => {
  const keys = entityQueryKeys("book-reviews");
  const bookReviewId = "c2c2c2c2-0002-4000-8000-000000000001";

  it("prefixes every key with the entities", () => {
    expect(keys.all).toEqual(["book-reviews"]);
    expect(keys.list({ page: 2 })).toEqual(["book-reviews", { page: 2 }]);
    expect(keys.byId(bookReviewId)).toEqual(["book-reviews", bookReviewId]);
    expect(keys.preview(bookReviewId)).toEqual(["book-reviews", bookReviewId, "preview"]);
    expect(keys.template()).toEqual(["book-reviews", "new"]);
    expect(keys.template("book-1")).toEqual(["book-reviews", "new", "book-1"]);
    expect(keys.check("isbn", "978-3-16-148410-0", bookReviewId)).toEqual([
      "book-reviews",
      "check",
      "isbn",
      "978-3-16-148410-0",
      bookReviewId,
    ]);
    expect(keys.of(bookReviewId, "comments", { page: 1 })).toEqual(["book-reviews", bookReviewId, "comments", { page: 1 }]);
  });

  it("makes the key of an entity a prefix of its preview and sub-resource keys", () => {
    const byId = keys.byId(bookReviewId);

    expect(keys.preview(bookReviewId).slice(0, byId.length)).toEqual(byId);
    expect(keys.of(bookReviewId, "comments").slice(0, byId.length)).toEqual(byId);
  });
});
