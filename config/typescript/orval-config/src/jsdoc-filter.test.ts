/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { jsDocFilter } from "./jsdoc-filter";

/**
 * Tests
 */

describe("jsDocFilter", () => {
  it("should return empty array for null schema", () => {
    expect(jsDocFilter(null)).toEqual([]);
  });

  it("should return empty array for undefined schema", () => {
    expect(jsDocFilter(undefined)).toEqual([]);
  });

  it("should filter allowed properties from schema", () => {
    const schema = {
      description: "A user object",
      type: "object", // should be filtered out (redundant with TS types)
      properties: { name: { type: "string" } }, // should be filtered out
      additionalProperties: false, // should be filtered out
    };

    const result = jsDocFilter(schema);

    expect(result).toHaveLength(1);
    expect(result.find((e) => e.key === "description")).toBeDefined();
    expect(result.find((e) => e.key === "type")).toBeUndefined();
    expect(result.find((e) => e.key === "properties")).toBeUndefined();
  });

  it("should include all constraint properties", () => {
    const schema = {
      type: "string", // filtered out (redundant)
      minLength: 1,
      maxLength: 100,
      pattern: "^[a-z]+$",
    };

    const result = jsDocFilter(schema);

    expect(result).toHaveLength(3);
    expect(result.find((e) => e.key === "minLength")?.value).toBe("1");
    expect(result.find((e) => e.key === "maxLength")?.value).toBe("100");
    expect(result.find((e) => e.key === "pattern")?.value).toBe("^[a-z]+$");
  });

  it("should include number constraints", () => {
    const schema = {
      type: "number",
      minimum: 0,
      maximum: 100,
      exclusiveMinimum: true,
      exclusiveMaximum: false,
      multipleOf: 0.5,
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "minimum")?.value).toBe("0");
    expect(result.find((e) => e.key === "maximum")?.value).toBe("100");
    expect(result.find((e) => e.key === "multipleOf")?.value).toBe("0.5");
  });

  it("should include enum values", () => {
    const schema = {
      type: "string",
      enum: ["active", "inactive", "pending"],
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "enum")?.value).toBe("active, inactive, pending");
  });

  it("should include custom x- extensions", () => {
    const schema = {
      type: "string",
      "x-source-kotlin-type": "kotlin.String",
      "x-source-location": "src/main/kotlin/Model.kt:42",
      "x-unknown-extension": "ignored", // should be filtered out
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "x-source-kotlin-type")?.value).toBe("kotlin.String");
    expect(result.find((e) => e.key === "x-source-location")?.value).toBe("src/main/kotlin/Model.kt:42");
    expect(result.find((e) => e.key === "x-unknown-extension")).toBeUndefined();
  });

  it("should include deprecated flag", () => {
    const schema = {
      type: "string",
      deprecated: true,
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "deprecated")?.value).toBe("true");
  });

  it("should include nullable flag", () => {
    const schema = {
      type: "string",
      nullable: true,
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "nullable")?.value).toBe("true");
  });

  it("should include default and example values", () => {
    const schema = {
      type: "string",
      default: "hello",
      example: "world",
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "default")?.value).toBe("hello");
    expect(result.find((e) => e.key === "example")?.value).toBe("world");
  });

  it("should sort entries with description first", () => {
    const schema = {
      type: "string",
      minimum: 0,
      description: "A test property",
      deprecated: true,
    };

    const result = jsDocFilter(schema);

    // description should come first, then deprecated, then type, then minimum
    expect(result[0]?.key).toBe("description");
    expect(result[1]?.key).toBe("deprecated");
  });

  it("should include array constraints", () => {
    const schema = {
      type: "array",
      minItems: 1,
      maxItems: 10,
      uniqueItems: true,
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "minItems")?.value).toBe("1");
    expect(result.find((e) => e.key === "maxItems")?.value).toBe("10");
    expect(result.find((e) => e.key === "uniqueItems")?.value).toBe("true");
  });

  it("should include title and summary", () => {
    const schema = {
      title: "User",
      summary: "Represents a user in the system",
      type: "object",
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "title")?.value).toBe("User");
    expect(result.find((e) => e.key === "summary")?.value).toBe("Represents a user in the system");
  });

  it("should include format property", () => {
    const schema = {
      type: "string",
      format: "date-time",
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "format")?.value).toBe("date-time");
  });

  it("should include value class unit metadata", () => {
    const schema = {
      type: "number",
      format: "double",
      "x-source-kotlin-type": "it.neckar.template.react.service.model.EggWeight",
      "x-unit-symbol": "g",
      "x-unit-name": "gram",
      "x-german-term": "Eigewicht",
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "x-source-kotlin-type")?.value).toBe("it.neckar.template.react.service.model.EggWeight");
    expect(result.find((e) => e.key === "x-unit-symbol")?.value).toBe("g");
    expect(result.find((e) => e.key === "x-unit-name")?.value).toBe("gram");
    expect(result.find((e) => e.key === "x-german-term")?.value).toBe("Eigewicht");
  });

  it("should include x-unit for property-level unit annotations", () => {
    const schema = {
      type: "integer",
      description: "Age in months",
      "x-unit": "mo",
    };

    const result = jsDocFilter(schema);

    expect(result.find((e) => e.key === "x-unit")?.value).toBe("mo");
  });
});
