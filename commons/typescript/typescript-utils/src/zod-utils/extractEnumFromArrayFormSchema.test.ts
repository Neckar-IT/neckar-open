/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { createZodEnumFromConstObject } from "./createZodEnumFromConstObject";
import { extractEnumFromArrayFormSchema } from "./extractEnumFromArrayFormSchema";

/**
 * Tests
 */

describe("extractEnumFromArrayFormSchema", () => {
  const ChickenBreed = {
    Silkie: "Silkie",
    Leghorn: "Leghorn",
  } as const;

  const formSchema = z.object({
    breeds: z.array(createZodEnumFromConstObject(ChickenBreed)),
    optionalBreeds: z.array(createZodEnumFromConstObject(ChickenBreed).optional()).nullable(),
    breed: createZodEnumFromConstObject(ChickenBreed),
    names: z.array(z.string()),
  });

  it("extracts the enum of an array field", () => {
    expect(extractEnumFromArrayFormSchema(formSchema, "breeds")).toEqual(ChickenBreed);
  });

  it("unwraps a nullable array and an optional element", () => {
    expect(extractEnumFromArrayFormSchema(formSchema, "optionalBreeds")).toEqual(ChickenBreed);
  });

  it("throws for an enum field that is no array", () => {
    expect(() => extractEnumFromArrayFormSchema(formSchema, "breed")).toThrow("Field 'breed' is not an array of an enum.");
  });

  it("throws for an array of strings", () => {
    expect(() => extractEnumFromArrayFormSchema(formSchema, "names")).toThrow("Field 'names' is not an array of an enum.");
  });
});
