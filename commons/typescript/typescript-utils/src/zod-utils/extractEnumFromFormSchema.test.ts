/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { createZodEnumFromConstObject } from "./createZodEnumFromConstObject";
import { extractEnumFromFormSchema } from "./extractEnumFromFormSchema";

/**
 * Tests
 */

describe("extractEnumFromFormSchema", () => {
  const ChickenBreed = {
    Silkie: "Silkie",
    RhodeIslandRed: "RhodeIslandRed",
    AyamCemani: "AyamCemani",
    Leghorn: "Leghorn",
    Orpington: "Orpington",
  } as const;

  const formSchema = z.object({
    breed: createZodEnumFromConstObject(ChickenBreed),
    name: z.string(),
  });

  it("extracts the enum correctly", () => {
    const extractedEnum = extractEnumFromFormSchema(formSchema, "breed");
    expect(extractedEnum).toEqual(ChickenBreed);
  });

  it("throws an error if the field is not an enum", () => {
    expect(() => extractEnumFromFormSchema(formSchema, "name")).toThrow("Field 'name' is not an enum.");
  });
});
