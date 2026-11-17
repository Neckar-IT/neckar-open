import { QueryClient } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";
import { refetchQueriesExceptPrevented, resetQueriesExceptPrevented } from "./resetQueriesExceptPrevented";

describe("resetQueriesExceptPrevented", () => {
  it("resets queries without the flag and keeps queries with preventBeingResetByMutations", async () => {
    // gcTime Infinity: no garbage-collection timer outlives the test.
    const client = new QueryClient({ defaultOptions: { queries: { gcTime: Infinity } } });
    await client.query({ queryKey: ["books"], queryFn: async () => "book" });
    await client.query({ queryKey: ["getMe"], queryFn: async () => "me", meta: { preventBeingResetByMutations: true } });

    await resetQueriesExceptPrevented(client);

    expect(client.getQueryData(["books"])).toBeUndefined();
    expect(client.getQueryData(["getMe"])).toBe("me");
  });
});

describe("refetchQueriesExceptPrevented", () => {
  it("keeps the data of a query without the flag and marks it stale, and leaves a flagged query alone", async () => {
    const client = new QueryClient({ defaultOptions: { queries: { gcTime: Infinity } } });
    await client.query({ queryKey: ["books"], queryFn: async () => "book" });
    await client.query({ queryKey: ["getMe"], queryFn: async () => "me", meta: { preventBeingResetByMutations: true } });

    await refetchQueriesExceptPrevented(client);

    expect(client.getQueryData(["books"])).toBe("book");
    expect(client.getQueryState(["books"])?.isInvalidated).toBe(true);
    expect(client.getQueryState(["getMe"])?.isInvalidated).toBe(false);
  });
});
