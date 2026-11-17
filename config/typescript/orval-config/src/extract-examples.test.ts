/* eslint-disable quotes */

/**
 * Imports
 */

import type { GeneratorOptions, GeneratorVerbOptions } from "@orval/core";
import * as fs from "fs";
import * as os from "os";
import path from "path";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { extractExamples, extractSchemaExamples, writeExampleFile } from "./extract-examples";

/**
 * Helpers
 */

function verbOptionsWithExample(typeRef: string): GeneratorVerbOptions {
  return {
    response: {
      types: {
        success: [{ examples: { good: { "x-type-ref": typeRef, value: { type: "good" } } } }],
      },
    },
  } as unknown as GeneratorVerbOptions;
}

function generatorOptionsFor(orvalOutput: string): GeneratorOptions {
  return { output: orvalOutput, context: { output: { mode: "split" } } } as unknown as GeneratorOptions;
}

/**
 * Tests
 */

describe("extract-examples", () => {
  let outputDirectory: string;

  beforeEach(() => {
    outputDirectory = fs.mkdtempSync(path.join(os.tmpdir(), "extract-examples-"));
  });

  afterEach(() => {
    fs.rmSync(outputDirectory, { recursive: true, force: true });
  });

  it("should create a file for the example", () => {
    const example = {
      "x-type-ref": "#/components/schemas/Example.Success.WithType",
      value: {
        type: "good",
        data: {
          string: "this is a string",
          number: 42,
          array: ["one", "two", "three"],
        },
      },
    };

    writeExampleFile(example, `${outputDirectory}/example.ts`, "split");

    let expectedOutput = 'import { type ExampleSuccessWithType } from "../example.schemas";\n';
    expectedOutput += "\n";
    // Uses type assertion to bypass strict null checks for example data
    expectedOutput += "export const exampleSuccessWithType = {\n";
    expectedOutput += '  type: "good",\n';
    expectedOutput += "  data: {\n";
    expectedOutput += '    string: "this is a string",\n';
    expectedOutput += "    number: 42,\n";
    expectedOutput += "    array: [\n";
    expectedOutput += '      "one",\n';
    expectedOutput += '      "two",\n';
    expectedOutput += '      "three"\n';
    expectedOutput += "    ]\n";
    expectedOutput += "  }\n";
    expectedOutput += "} as unknown as ExampleSuccessWithType;\n";

    expect(fs.readFileSync(`${outputDirectory}/examples/exampleSuccessWithType.example.ts`, "utf-8")).toBe(expectedOutput);
  });

  it("should remove an example file of a previous run", () => {
    fs.mkdirSync(`${outputDirectory}/examples`);
    fs.writeFileSync(`${outputDirectory}/examples/removedVariantWithType.example.ts`, "");

    extractExamples(verbOptionsWithExample("#/components/schemas/Current.Good"), generatorOptionsFor(`${outputDirectory}/api.ts`));

    expect(fs.readdirSync(`${outputDirectory}/examples`)).toEqual(["currentGood.example.ts"]);
  });

  it("should keep the example files another Orval project wrote earlier in the same run", () => {
    extractExamples(verbOptionsWithExample("#/components/schemas/Default.Good"), generatorOptionsFor(`${outputDirectory}/default.ts`));
    extractExamples(verbOptionsWithExample("#/components/schemas/Admin.Good"), generatorOptionsFor(`${outputDirectory}/admin.ts`));

    expect(fs.readdirSync(`${outputDirectory}/examples`).toSorted()).toEqual(["adminGood.example.ts", "defaultGood.example.ts"]);
  });

  it("should write one file per schema-level example, named after the example id", () => {
    const specPath = `${outputDirectory}/spec.json`;
    fs.writeFileSync(
      specPath,
      JSON.stringify({
        paths: {},
        components: {
          schemas: {
            Context: {
              "x-examples": {
                bookContext: { value: { data: { type: "book" } } },
                authorContext: { value: { data: { type: "author" } } },
              },
            },
            WithoutExamples: { type: "object" },
          },
        },
      }),
    );

    extractSchemaExamples(specPath, `${outputDirectory}/context.ts`, "single");

    expect(fs.readdirSync(`${outputDirectory}/examples`).toSorted()).toEqual(["authorContext.example.ts", "bookContext.example.ts"]);
    expect(fs.readFileSync(`${outputDirectory}/examples/bookContext.example.ts`, "utf-8")).toBe(
      'import { type Context } from "../context";\n\nexport const bookContext: Context = {\n  data: {\n    type: "book"\n  }\n};\n',
    );
  });
});
