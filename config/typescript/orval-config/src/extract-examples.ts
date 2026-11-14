/**
 * Imports
 */

import type { ClientMockGeneratorBuilder, GeneratorOptions, GeneratorVerbOptions, OutputMode } from "@orval/core";
import { camelCase, pascalCase } from "es-toolkit/string";
import * as fs from "fs";
import path from "path";

/**
 * Extract Examples from the OpenAPI document.
 *
 * This will generate a file with a typescript object generated from the examples as defined in the OpenAPI JSON.
 * It requires an additional type information (`x-type-ref`) to be defined alongside the example data.
 */

export function extractExamples(verbOptions: GeneratorVerbOptions, generatorOptions: GeneratorOptions): ClientMockGeneratorBuilder {
  const defaultReturn = {
    imports: [],
    implementation: {
      function: "const DO_NOT_USE = () => undefined",
      handlerName: "DO_NOT_USE",
      handler: "",
    },
  };

  clearExamplesDirectoryOncePerRun(generatorOptions.output);

  const examples = verbOptions.response.types.success[0]?.examples;

  if (!examples || examples.length === 0 || Array.isArray(examples)) {
    return defaultReturn;
  }

  for (const exampleName in examples) {
    const example = examples[exampleName] as unknown as Example;
    writeExampleFile(example, generatorOptions.output, generatorOptions.context.output.mode);
  }

  return defaultReturn;
}

/**
 * Extract the schema-level examples of an OpenAPI document that has no routes.
 *
 * `mock: extractExamples` runs per operation, so examples of a type without a route never reach it. This
 * reads `components.schemas.<Type>.x-examples` instead and writes one file per example, named after the
 * example id; the type is the schema the example hangs on. Run it from the `afterAllFilesWrite` hook of the
 * Orval project, after the types are written.
 */
export function extractSchemaExamples(specPath: string, orvalOutput: string, orvalMode: OutputMode): void {
  const spec = JSON.parse(fs.readFileSync(specPath, "utf-8")) as OpenApiDocument;

  clearExamplesDirectoryOncePerRun(orvalOutput);

  for (const [schemaName, schema] of Object.entries(spec.components?.schemas ?? {})) {
    for (const [exampleId, example] of Object.entries(schema["x-examples"] ?? {})) {
      writeExampleFile({ "x-type-ref": `#/components/schemas/${schemaName}`, value: example.value }, orvalOutput, orvalMode, camelCase(exampleId));
    }
  }
}

interface OpenApiDocument {
  components?: { schemas?: Record<string, { "x-examples"?: Record<string, { value: TSObject }> }> };
}

/**
 * Clear the examples directory once per Orval run, so it holds only the examples of the specs just read
 */

// Keyed by directory: the Orval projects of one config share an examples directory within one process.
const clearedExamplesDirectories = new Set<string>();

function clearExamplesDirectoryOncePerRun(orvalOutput: string): void {
  const directory = examplesDirectory(orvalOutput);
  if (clearedExamplesDirectories.has(directory)) {
    return;
  }
  fs.rmSync(directory, { recursive: true, force: true });
  clearedExamplesDirectories.add(directory);
}

function examplesDirectory(orvalOutput: string): string {
  return path.join(path.dirname(orvalOutput), "examples");
}

/**
 * Write the actual example file
 */

interface Example {
  "x-type-ref": string;
  value: TSObject;
}

/**
 * Writes `<fileBaseName>.example.ts`; the name of the type is the base name unless [fileBaseName] is given.
 *
 * With [fileBaseName] the constant is annotated with the type, so the compiler checks the example. Without it
 * the value is asserted: Orval generates `property?: string` for optional nullable properties, but the JSON
 * examples of a route contain explicit null values, which only an assertion accepts.
 */
export function writeExampleFile(example: Example, orvalOutput: string, orvalMode: OutputMode, fileBaseName?: string): void {
  const typeRef = example["x-type-ref"];
  if (!typeRef) {
    console.warn(`No "x-type-ref" found for example: ${JSON.stringify(example)}`);
    return;
  }

  const lastTypeRefPart = typeRef.split("/").at(-1);
  if (!lastTypeRefPart) {
    throw new Error(`Cannot derive an interface name from "x-type-ref": ${typeRef}`);
  }
  const interfaceName = pascalCase(lastTypeRefPart);

  const value = example.value;

  const schemasFileName = path.basename(orvalOutput);
  if (!schemasFileName) {
    throw new Error(`Cannot derive the schemas file name from the Orval output path: ${orvalOutput}`);
  }
  const schemasFileRelativePath =
    orvalMode === "split" ? `../${schemasFileName.replace(".ts", ".schemas")}` : `../${schemasFileName.replace(".ts", "")}`;

  const outputFilePath = path.join(examplesDirectory(orvalOutput), `${fileBaseName ?? camelCase(interfaceName)}.example.ts`);

  let content = "";
  content += `import { type ${interfaceName} } from "${schemasFileRelativePath}";\n`;
  content += "\n";
  const constantName = fileBaseName ?? camelCase(interfaceName);
  if (fileBaseName) {
    content += `export const ${constantName}: ${interfaceName} = ${toTSObjectLiteral(value)};\n`;
  } else {
    content += `export const ${constantName} = ${toTSObjectLiteral(value)} as unknown as ${interfaceName};\n`;
  }

  // create example file
  createFileWithDirs(outputFilePath, content);
}

/**
 * Create a string representation of a typescript object
 */

type Primitive = string | number | boolean | null;
type TSObject = Primitive | TSObject[] | { [key: string]: TSObject };

function toTSObjectLiteral(obj: TSObject, indent = 2): string {
  const space = " ".repeat(indent);

  if (obj === null) {
    return "null";
  }

  if (typeof obj === "string") {
    return JSON.stringify(obj);
  }

  if (typeof obj === "number" || typeof obj === "boolean") {
    return obj.toString();
  }

  if (Array.isArray(obj)) {
    const arrayItems = obj.map((item) => toTSObjectLiteral(item, indent + 2));
    return `[\n${space}${arrayItems.join(`,\n${space}`)}\n${" ".repeat(indent - 2)}]`;
  }

  if (typeof obj === "object") {
    const entries = Object.entries(obj).map(([key, value]) => {
      // Only quote keys if they are NOT valid identifiers
      const validKey = /^[a-zA-Z_$][a-zA-Z0-9_$]*$/.test(key) ? key : `"${key}"`;
      return `${space}${validKey}: ${toTSObjectLiteral(value, indent + 2)}`;
    });
    return `{\n${entries.join(",\n")}\n${" ".repeat(indent - 2)}}`;
  }

  return "undefined"; // fallback
}

/**
 * Write a file and generate all necessary parent directories
 */

function createFileWithDirs(filePath: string, content = ""): void {
  const dir = path.dirname(filePath);

  // Create all parent directories if they don't exist
  fs.mkdirSync(dir, { recursive: true });

  // Write the file content (overwrites if it already exists)
  fs.writeFileSync(filePath, content);
}
