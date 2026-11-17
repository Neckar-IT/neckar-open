/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { slugify } from "./slugify";

/**
 * Tests
 */

describe("slugify", () => {
  it("should convert a string to a slug", () => {
    expect(slugify("Hello World!")).toBe("hello-world");
    expect(slugify("  Leading and trailing spaces  ")).toBe("leading-and-trailing-spaces");
    expect(slugify("Special characters !@#$%^&*()")).toBe("special-characters");
    expect(slugify("Multiple   spaces")).toBe("multiple-spaces");
    expect(slugify("Accented characters: é, è, ü")).toBe("accented-characters-e-e-u");
  });

  it("should handle empty strings", () => {
    expect(slugify("")).toBe("");
  });

  it("should handle strings with only spaces", () => {
    expect(slugify("     ")).toBe("");
  });

  it("should handle strings with only special characters", () => {
    expect(slugify("!@#$%^&*()")).toBe("");
  });

  it("should handle strings with multiple hyphens", () => {
    expect(slugify("Multiple   hyphens---here")).toBe("multiple-hyphens-here");
  });
});
