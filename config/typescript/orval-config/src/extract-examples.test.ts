/* eslint-disable quotes */

/**
 * Imports
 */

import * as fs from "fs";
import * as os from "os";
import path from "path";
import type { GeneratorOptions, GeneratorVerbOptions } from "@orval/core";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { extractExamples, extractSchemaExamples, writeRouteExampleFile, writeSchemaExampleFile } from "./extract-examples";

/**
 * Helpers
 */

function verbOptionsWithExample(typeRef: string, operationName = "GetExample", value: object = { type: "good" }): GeneratorVerbOptions {
  return {
    operationName,
    response: {
      types: {
        success: [{ examples: { good: { "x-type-ref": typeRef, value } } }],
      },
    },
  } as unknown as GeneratorVerbOptions;
}

const authorTypeRef = "#/components/schemas/FoundResponse.Good__AuthorDetails__.WithType";

function authorExample(operationName: string, name: string): GeneratorVerbOptions {
  return verbOptionsWithExample(authorTypeRef, operationName, { type: "good", data: { name } });
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

    writeRouteExampleFile(example, `${outputDirectory}/example.ts`, "split", "GetExample", "good");

    let expectedOutput = 'import { type ExampleSuccessWithType } from "../example.schemas";\n';
    expectedOutput += "\n";
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

  it("should keep the first value in the plain file and write a differing second value of another operation to its own file", () => {
    extractExamples(authorExample("GetAuthor", "Mark Twain"), generatorOptionsFor(`${outputDirectory}/api.ts`));
    extractExamples(authorExample("PostDuplicateForAuthor", "Mark Twain (copy)"), generatorOptionsFor(`${outputDirectory}/api.ts`));

    expect(fs.readdirSync(`${outputDirectory}/examples`).toSorted()).toEqual([
      "foundResponseGoodAuthorDetailsWithType.example.ts",
      "foundResponseGoodAuthorDetailsWithTypeForPostDuplicateForAuthor.example.ts",
    ]);
    expect(fs.readFileSync(`${outputDirectory}/examples/foundResponseGoodAuthorDetailsWithType.example.ts`, "utf-8")).toContain(
      'name: "Mark Twain"\n',
    );
    const duplicate = fs.readFileSync(
      `${outputDirectory}/examples/foundResponseGoodAuthorDetailsWithTypeForPostDuplicateForAuthor.example.ts`,
      "utf-8",
    );
    expect(duplicate).toContain("export const foundResponseGoodAuthorDetailsWithTypeForPostDuplicateForAuthor = {\n");
    expect(duplicate).toContain('name: "Mark Twain (copy)"\n');
  });

  it("should write one file for the same value from two Orval projects sharing the examples directory", () => {
    extractExamples(authorExample("GetAuthor", "Mark Twain"), generatorOptionsFor(`${outputDirectory}/pattern-service.ts`));
    extractExamples(authorExample("GetAuthorForAdmin", "Mark Twain"), generatorOptionsFor(`${outputDirectory}/pattern-admin.ts`));

    expect(fs.readdirSync(`${outputDirectory}/examples`)).toEqual(["foundResponseGoodAuthorDetailsWithType.example.ts"]);
  });

  it("should write each further value of one operation to a file named after its example id", () => {
    const workflowTypeRef = "#/components/schemas/WorkflowStateResponse.Good.WithType";
    const workflow = (state: string) => ({ "x-type-ref": workflowTypeRef, value: { type: "good", data: { state } } });
    const verbOptions = {
      operationName: "GetWorkflow",
      response: { types: { success: [{ examples: { InPreparation: workflow("InPreparation"), QuoteSent: workflow("QuoteSent") } }] } },
    } as unknown as GeneratorVerbOptions;
    extractExamples(verbOptions, generatorOptionsFor(`${outputDirectory}/api.ts`));
    extractExamples(
      {
        ...verbOptions,
        operationName: "PutWorkflow",
        response: {
          types: { success: [{ examples: { a: workflow("A"), sameAsA: workflow("A"), b: workflow("B"), sameAsB: workflow("B") } }] },
        },
      } as unknown as GeneratorVerbOptions,
      generatorOptionsFor(`${outputDirectory}/api.ts`),
    );

    expect(fs.readdirSync(`${outputDirectory}/examples`).toSorted()).toEqual([
      "workflowStateResponseGoodWithType.example.ts",
      "workflowStateResponseGoodWithTypeForGetWorkflowQuoteSent.example.ts",
      "workflowStateResponseGoodWithTypeForPutWorkflow.example.ts",
      "workflowStateResponseGoodWithTypeForPutWorkflowB.example.ts",
    ]);
  });

  it("should write a differing value of an operation of the same name in another Orval project to its own file", () => {
    extractExamples(authorExample("GetAuthor", "Mark Twain"), generatorOptionsFor(`${outputDirectory}/pattern-service.ts`));
    extractExamples(authorExample("GetAuthor", "Samuel Clemens"), generatorOptionsFor(`${outputDirectory}/pattern-admin.ts`));

    expect(fs.readdirSync(`${outputDirectory}/examples`).toSorted()).toEqual([
      "foundResponseGoodAuthorDetailsWithType.example.ts",
      "foundResponseGoodAuthorDetailsWithTypeForGetAuthor.example.ts",
    ]);
  });

  it("should reject a third differing value under the same operation and example id", () => {
    const options = ["PostDuplicateForAuthor", "good"] as const;
    const example = (name: string) => ({ "x-type-ref": authorTypeRef, value: { type: "good", data: { name } } });
    writeRouteExampleFile(example("Mark Twain"), `${outputDirectory}/api.ts`, "split", "GetAuthor", "good");
    writeRouteExampleFile(example("Mark Twain (copy)"), `${outputDirectory}/api.ts`, "split", ...options);
    writeRouteExampleFile(example("Mark Twain (draft)"), `${outputDirectory}/api.ts`, "split", ...options);

    expect(() => writeRouteExampleFile(example("Mark Twain (third)"), `${outputDirectory}/api.ts`, "split", ...options)).toThrow(
      "Two examples named foundResponseGoodAuthorDetailsWithTypeForPostDuplicateForAuthorGood have different values",
    );
  });

  it("should reject two differing schema example values under one name", () => {
    const example = (name: string) => ({ "x-type-ref": "#/components/schemas/AuthorDetails", value: { name } });
    writeSchemaExampleFile(example("Mark Twain"), `${outputDirectory}/api.ts`, "split", "markTwain");

    expect(() => writeSchemaExampleFile(example("Samuel Clemens"), `${outputDirectory}/api.ts`, "split", "markTwain")).toThrow(
      "Two examples named markTwain have different values",
    );
  });

  it("should accept the same schema example value twice under one name", () => {
    const example = { "x-type-ref": "#/components/schemas/AuthorDetails", value: { name: "Mark Twain" } };
    writeSchemaExampleFile(example, `${outputDirectory}/api.ts`, "split", "markTwain");
    writeSchemaExampleFile(example, `${outputDirectory}/api.ts`, "split", "markTwain");

    expect(fs.readdirSync(`${outputDirectory}/examples`)).toEqual(["markTwain.example.ts"]);
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
                secondBookContext: { value: { data: { type: "book" } } },
                authorContext: { value: { data: { type: "author" } } },
              },
            },
            WithoutExamples: { type: "object" },
          },
        },
      }),
    );

    extractSchemaExamples(specPath, `${outputDirectory}/context.ts`, "single");

    expect(fs.readdirSync(`${outputDirectory}/examples`).toSorted()).toEqual([
      "authorContext.example.ts",
      "bookContext.example.ts",
      "secondBookContext.example.ts",
    ]);
    expect(fs.readFileSync(`${outputDirectory}/examples/bookContext.example.ts`, "utf-8")).toBe(
      'import { type Context } from "../context";\n\nexport const bookContext: Context = {\n  data: {\n    type: "book"\n  }\n};\n',
    );
  });
});
