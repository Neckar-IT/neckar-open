/**
 * What zod reports for a required string, spelled out as the exact issue objects.
 *
 * The issues are asserted whole rather than by count, because they are what a form renders: their
 * `path` selects the field to mark and their `message` is the text the user reads. A library upgrade
 * that rewords a message or renames a code changes the UI, and this is where that shows up.
 */

import { describe, expect, it } from "vitest";
import { z as zod } from "zod";

describe("zod-required-strings", () => {
  it("should produce an error if a string is not present in an object", () => {
    const schema = zod.object({
      name: zod.string(),
    });

    const result = schema.safeParse({});

    expect(result.success).toBe(false);
    expect(result.error?.issues).toStrictEqual([
      {
        code: "invalid_type",
        expected: "string",
        path: ["name"],
        message: "Invalid input: expected string, received undefined",
      },
    ]);
  });

  it("should produce an error with a custom message if a string is not present in an object", () => {
    // The bare string argument is the custom error message. It replaces the wording only, leaving
    // `code` and `expected` untouched, so anything switching on the code keeps working.
    const schema = zod.object({
      name: zod.string("The name is a required field."),
    });

    const result = schema.safeParse({});

    expect(result.success).toBe(false);
    expect(result.error?.issues).toStrictEqual([
      {
        code: "invalid_type",
        expected: "string",
        path: ["name"],
        message: "The name is a required field.",
      },
    ]);
  });

  it("should not produce an error if the string is marked as optional", () => {
    const schema = zod.object({
      name: zod.string().optional(),
    });

    const result = schema.safeParse({});

    expect(result.success).toBe(true);
  });

  it("should produce an error if the values is an empty string", () => {
    // A required string still accepts "". Rejecting the empty value takes an explicit rule, and there
    // are two spellings of the same one.
    const schema1 = zod.object({ name: zod.string().nonempty() });
    const schema2 = zod.object({ name: zod.string().min(1) });

    const result1 = schema1.safeParse({ name: "" });
    const result2 = schema2.safeParse({ name: "" });

    expect(result1.success).toBe(false);
    expect(result2.success).toBe(false);

    // .nonempty() is .min(1) under a friendlier name — down to the issue object, which reports
    // `too_small` with `minimum: 1` rather than a code of its own. Nothing downstream can tell the two
    // schemas apart, and a message meant for an empty field has to be attached to the length rule.
    expect(result1.error?.issues).toStrictEqual(result2.error?.issues);
    expect(result1.error?.issues).toStrictEqual([
      {
        code: "too_small",
        inclusive: true,
        message: "Too small: expected string to have >=1 characters",
        minimum: 1,
        path: ["name"],
        origin: "string",
      },
    ]);
  });
});
