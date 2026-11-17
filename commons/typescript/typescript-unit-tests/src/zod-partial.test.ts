/**
 * How far down a schema `.partial()` reaches.
 *
 * It reaches exactly one level. The name suggests "everything in here is now optional", which is what
 * a draft or a PATCH payload would need, and that is not what it does.
 */

import { describe, expect, it } from "vitest";
import { z } from "zod";

describe("zod-partial", () => {
  it("should mark a field as optional", () => {
    const schema = z.object({
      name: z.string().nonempty(),
    });

    expect(schema.safeParse({}).success).toBe(false);
    expect(schema.safeParse({ name: "John" }).success).toBe(true);

    const partialSchema = schema.partial();

    expect(partialSchema.safeParse({}).success).toBe(true);
    expect(partialSchema.safeParse({ name: "John" }).success).toBe(true);
  });

  it("does not mark nested objects as optional", () => {
    const schema = z.object({
      address: z.object({
        street: z.string().nonempty(),
        city: z.string().nonempty(),
      }),
    });

    expect(schema.safeParse({}).success).toBe(false);
    expect(schema.safeParse({ address: { street: "Main St", city: "Springfield" } }).success).toBe(true);

    const partialSchema = schema.partial();

    // Only `address` itself became optional. Leaving it out passes; supplying it means supplying a
    // complete address, because `street` and `city` are still required inside it. So a partial schema
    // does not accept a half-filled nested object — nesting has to be made optional by calling
    // .partial() on the inner schema as well.
    expect(partialSchema.safeParse({}).success).toBe(true);
    expect(partialSchema.safeParse({ address: {} }).success).toBe(false);
  });
});
