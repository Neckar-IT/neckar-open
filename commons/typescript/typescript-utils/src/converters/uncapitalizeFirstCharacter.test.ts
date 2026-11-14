import { describe, expect, it } from "vitest";
import { uncapitalizeFirstCharacter } from "./uncapitalizeFirstCharacter";

describe("uncapitalizeFirstCharacter", () => {
  it("lowercases only the first character", () => {
    expect(uncapitalizeFirstCharacter("MissingFirstName")).toBe("missingFirstName");
  });

  it("returns an empty string unchanged", () => {
    expect(uncapitalizeFirstCharacter("")).toBe("");
  });
});
