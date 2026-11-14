import { describe, expect, it } from "vitest";
import { entityReferenceIdSchema } from "./entityReferenceIdSchema";

describe("entityReferenceIdSchema", () => {
  it("accepts a demo id with zeroed version and variant nibbles", () => {
    expect(entityReferenceIdSchema.safeParse("c2c2c2c2-0002-0002-0002-000000000003").success).toBe(true);
  });

  it("rejects input that is no id", () => {
    expect(entityReferenceIdSchema.safeParse("").success).toBe(false);
    expect(entityReferenceIdSchema.safeParse("Large Print").success).toBe(false);
  });
});
