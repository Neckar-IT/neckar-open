/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { capitalizeFirstCharacter } from "./capitalizeFirstCharacter";

/**
 * Tests
 */

describe("capitalizeFirstCharacter", () => {
  it("should capitalize the first character of a string", () => {
    expect(capitalizeFirstCharacter("hello")).toBe("Hello");
    expect(capitalizeFirstCharacter("world")).toBe("World");
  });
});
