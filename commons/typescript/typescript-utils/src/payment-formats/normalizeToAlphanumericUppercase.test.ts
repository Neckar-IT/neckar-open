import { describe, expect, it } from "vitest";
import { normalizeToAlphanumericUppercase } from "./normalizeToAlphanumericUppercase";

describe("normalizeToAlphanumericUppercase", () => {
  it("drops spaces, dashes and dots between the groups", () => {
    expect(normalizeToAlphanumericUppercase(" DE89 3704-0044.0532 0130 00 ")).toBe("DE89370400440532013000");
  });

  it("uppercases lowercase letters", () => {
    expect(normalizeToAlphanumericUppercase("cobadeffxxx")).toBe("COBADEFFXXX");
  });

  it("keeps non-ASCII letters as Kotlin's isLetterOrDigit does", () => {
    expect(normalizeToAlphanumericUppercase("bä-1")).toBe("BÄ1");
  });
});
