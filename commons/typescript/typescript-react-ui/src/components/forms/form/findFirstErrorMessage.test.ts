import { describe, expect, it } from "vitest";
import { findFirstErrorMessage } from "./findFirstErrorMessage";

describe("findFirstErrorMessage", () => {
  it("returns undefined when value is nullish", () => {
    expect(findFirstErrorMessage(undefined)).toBeUndefined();
    expect(findFirstErrorMessage(null)).toBeUndefined();
  });

  it("returns value when it is already a string", () => {
    expect(findFirstErrorMessage("error")).toBe("error");
  });

  it("prefers direct message on the object", () => {
    expect(findFirstErrorMessage({ message: "field error" })).toBe("field error");
  });

  it("handles non-string message values", () => {
    expect(findFirstErrorMessage({ message: 123 })).toBe("123");
  });

  it("walks types entries before other nested keys", () => {
    expect(
      findFirstErrorMessage({
        types: {
          required: { message: "from types" },
        },
        nested: { message: "later entry" },
      }),
    ).toBe("from types");
  });

  it("returns first message found in nested descendants", () => {
    expect(
      findFirstErrorMessage({
        nested: [{ deeper: { message: "first" } }, { message: "second" }],
      }),
    ).toBe("first");
  });

  it("ignores react-hook-form metadata keys when descending", () => {
    expect(
      findFirstErrorMessage({
        ref: {},
        type: "required",
        message: undefined,
        other: { message: "usable" },
      }),
    ).toBe("usable");
  });
});
