/**
 * Imports
 */

import type { OpenApiDocument } from "@orval/core";
import { describe, expect, it } from "vitest";
import { inputTransformer } from "./input-transformer";

/**
 * Tests
 */

function specWithSchemas(schemas: Record<string, unknown>): OpenApiDocument {
  return { openapi: "3.1.0", info: { title: "Test", version: "1" }, paths: {}, components: { schemas } } as OpenApiDocument;
}

describe("inputTransformer", () => {
  it("prepends the Kotlin type and the German term to the description of a schema", () => {
    const spec = specWithSchemas({
      ContactPerson: {
        type: "object",
        description: "One contact person of a publisher.",
        "x-source-kotlin-type": "it.neckar.patterns.publisher.ContactPerson",
        "x-german-term": "Ansprechpartner",
      },
    });

    const schema = inputTransformer(spec).components?.schemas?.ContactPerson as { description?: string };

    expect(schema.description).toBe(
      "x-source-kotlin-type: it.neckar.patterns.publisher.ContactPerson\nx-german-term: Ansprechpartner\n\nOne contact person of a publisher.",
    );
  });

  it("adds the German term of each enum value to its description", () => {
    const spec = specWithSchemas({
      ContactRole: {
        type: "string",
        enum: ["Editorial", "Sales", "Press"],
        "x-enum-descriptions": { Editorial: "Editorial office.", Press: "Press office." },
        "x-enum-german-terms": { Editorial: "Lektorat", Sales: "Vertrieb" },
      },
    });

    const schema = inputTransformer(spec).components?.schemas?.ContactRole as { "x-enum-descriptions"?: Record<string, string> };

    expect(schema["x-enum-descriptions"]).toEqual({
      Editorial: "Editorial office. (x-german-term: Lektorat)",
      Sales: "x-german-term: Vertrieb",
      Press: "Press office.",
    });
  });
});
