/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { generateUuid } from "./generateUuid";

/**
 * Tests
 */

describe("generateUuid", () => {
  it("should return a new UUID", () => {
    const uuid = generateUuid();
    expect(uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  });
});
