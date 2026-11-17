import { describe, expect, it } from "vitest";
import { storybookStoryRoute } from "./storybookStoryRoute";

describe("storybookStoryRoute", () => {
  it("reads path, url and params of the story parameters", () => {
    const route = storybookStoryRoute({ layout: "fullscreen", path: "/projects/:projectId", params: { projectId: "a1", page: 2 } });

    expect(route).toEqual({ path: "/projects/:projectId", url: undefined, params: { projectId: "a1", page: 2 } });
  });

  it("reads the url of the story parameters", () => {
    expect(storybookStoryRoute({ url: "/projects/a1?tab=location" }).url).toBe("/projects/a1?tab=location");
  });

  it("throws for a path of another type", () => {
    expect(() => storybookStoryRoute({ path: 42 })).toThrow("Story parameter `path` is a number, expected a string");
  });

  it("throws for params given as an array", () => {
    expect(() => storybookStoryRoute({ params: ["a1"] })).toThrow("Story parameter `params` is no object of strings and numbers");
  });

  it("throws for params holding a boolean", () => {
    expect(() => storybookStoryRoute({ params: { archived: true } })).toThrow(
      "Story parameter `params` is no object of strings and numbers",
    );
  });
});
