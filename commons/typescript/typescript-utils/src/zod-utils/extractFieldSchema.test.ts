/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { extractFieldSchema } from "./extractFieldSchema";

/**
 * Zod Schema
 */

const schema = z.object({
  name: z.string(),
  age: z.number().optional(),
  address: z.object({
    street: z.string(),
    city: z.string().optional(),
  }),
  contact: z.discriminatedUnion("type", [
    z.object({
      type: z.literal("email"),
      email: z.string(),
    }),
    z.object({
      type: z.literal("phone"),
      phone: z.string(),
    }),
  ]),
  emails: z.array(z.email().nonempty()),
});

/**
 * Tests
 */

describe("extractFieldSchema", () => {
  it("should extract a top-level field", () => {
    const result = extractFieldSchema(schema, "name");
    expect(result).toBeInstanceOf(z.ZodString);
    expect(result?.safeParse("Alice").success).toBe(true);
    expect(result?.safeParse(123).success).toBe(false);
  });

  it("should preserve optional wrapper for optional fields", () => {
    const result = extractFieldSchema(schema, "age");
    expect(result).toBeInstanceOf(z.ZodOptional);
    expect(result?.safeParse(30).success).toBe(true);
    expect(result?.safeParse(undefined).success).toBe(true);
  });

  it("should extract nested fields", () => {
    const result = extractFieldSchema(schema, "address.street");
    expect(result).toBeInstanceOf(z.ZodString);
    expect(result?.safeParse("123 Main St").success).toBe(true);
    expect(result?.safeParse(456).success).toBe(false);
  });

  it("should handle discriminated unions", () => {
    const result = extractFieldSchema(schema, "contact.email");
    expect(result).toBeInstanceOf(z.ZodString);
  });

  it("should handle array fields", () => {
    const result = extractFieldSchema(schema, "emails.0");
    expect(result).toBeInstanceOf(z.ZodEmail);
    expect(result?.safeParse("test@email.com").success).toBe(true);
    expect(result?.safeParse("invalid-email").success).toBe(false);
  });
});
