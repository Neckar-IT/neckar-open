import { describe, expect, it } from "vitest";
import { transformDev } from "./transformDev";

describe("transformDev", () => {
  it("should wrap a simple key in brackets", () => {
    expect(transformDev("save")).toBe("[save]");
  });

  it("should wrap a dotted key path in brackets", () => {
    expect(transformDev("common.actions.delete")).toBe("[common.actions.delete]");
  });

  it("should wrap a namespace-prefixed key in brackets", () => {
    expect(transformDev("test:greeting")).toBe("[test:greeting]");
  });

  it("should wrap an empty key in brackets", () => {
    expect(transformDev("")).toBe("[]");
  });
});
