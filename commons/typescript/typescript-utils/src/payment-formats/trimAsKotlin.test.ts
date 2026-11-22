import { describe, expect, it } from "vitest";
import { trimAsKotlin } from "./trimAsKotlin";

describe("trimAsKotlin", () => {
  it("strips U+001C to U+001F on both sides", () => {
    expect(trimAsKotlin("\u001c\u001d4111\u001e\u001f")).toBe("4111");
  });

  it("strips spaces, tabs and line breaks on both sides", () => {
    expect(trimAsKotlin(" \t\n4111\r\n ")).toBe("4111");
  });

  it("keeps the characters between the first and the last non-blank one", () => {
    expect(trimAsKotlin(" 4111 \u001f 1111 ")).toBe("4111 \u001f 1111");
  });

  it("returns an empty string for blank input", () => {
    expect(trimAsKotlin("\u001f \u001c")).toBe("");
  });

  it("keeps U+001B, which Kotlin's trim keeps as well", () => {
    expect(trimAsKotlin("\u001b4111\u001b")).toBe("\u001b4111\u001b");
  });
});
