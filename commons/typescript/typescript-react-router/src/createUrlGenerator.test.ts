/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { createUrlGenerator } from "./createUrlGenerator";
import type { NoParams } from "./types";

/**
 * AppRoutes Type
 */

type AppRoutes = {
  "/todos": NoParams;
  "/todos/:todoId": { todoId: string };
  "/u/:id/m/:id": { id: string };
};

/**
 * Tests
 */

describe("createUrlGenerator", () => {
  it("should return a router path if no parameters are needed", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos" })).toBe("/todos");
  });

  it("should replace the route param placeholders with the actual parameters", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos/:todoId", params: { todoId: "42" } })).toBe("/todos/42");
  });

  it("should replace every occurrence of a repeated path param", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/u/:id/m/:id", params: { id: "abc" } })).toBe("/u/abc/m/abc");
  });

  it("should append search params in the order they were given", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos", searchParams: { status: "open", sort: "dueDate" } })).toBe("/todos?status=open&sort=dueDate");
  });

  it("should drop the search params that have no value", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos", searchParams: { status: "open", assignee: undefined } })).toBe("/todos?status=open");
  });

  it("should not append a question mark if no search param is left", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos", searchParams: {} })).toBe("/todos");
    expect(generateUrl({ to: "/todos", searchParams: { assignee: undefined } })).toBe("/todos");
    expect(generateUrl({ to: "/todos", searchParams: new URLSearchParams() })).toBe("/todos");
  });

  it("should take the search params from a URLSearchParams, including a repeated key", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    const searchParams = new URLSearchParams([
      ["tag", "urgent"],
      ["tag", "home"],
    ]);

    expect(generateUrl({ to: "/todos", searchParams })).toBe("/todos?tag=urgent&tag=home");
  });

  it("should encode the search param values", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos", searchParams: { query: "milk & eggs" } })).toBe("/todos?query=milk+%26+eggs");
  });

  it("should combine path params and search params", () => {
    const generateUrl = createUrlGenerator<AppRoutes>();

    expect(generateUrl({ to: "/todos/:todoId", params: { todoId: "42" }, searchParams: { view: "detail" } })).toBe("/todos/42?view=detail");
  });
});
