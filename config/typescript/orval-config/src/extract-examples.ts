/**
 * Imports
 */

import * as fs from "fs";
import path from "path";
import type { ClientMockGeneratorBuilder, GeneratorOptions, GeneratorVerbOptions, OutputMode } from "@orval/core";
import { camelCase, pascalCase } from "es-toolkit/string";

/**
 * Extract Examples from the OpenAPI document.
 *
 * Writes a file per example value that has none yet (see `writeRouteExampleFile`).
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

  if (!examples || Array.isArray(examples)) {
    return defaultReturn;
  }

  for (const exampleName in examples) {
    const example = examples[exampleName] as unknown as Example;
    writeRouteExampleFile(example, generatorOptions.output, generatorOptions.context.output.mode, verbOptions.operationName, exampleName);
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
      writeSchemaExampleFile(
        { "x-type-ref": `#/components/schemas/${schemaName}`, value: example.value },
        orvalOutput,
        orvalMode,
        camelCase(exampleId),
      );
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

// Every route example file of this process by path, with the Orval output and operation that wrote it.
const writtenRouteExampleFiles = new Map<string, string>();

// Examples directory, type and value of every route example of this process: a value that has a file gets no second one.
// Orval projects sharing an examples directory import the type from different schema files, so the import is no part of it.
const writtenRouteExampleValues = new Set<string>();

/**
 * Writes the first example of a type to `<type>.example.ts`; a later one with another value gets its own file (README.md).
 * The value is asserted instead of typed: route JSON holds explicit nulls where Orval generates optional properties.
 */
export function writeRouteExampleFile(
  example: Example,
  orvalOutput: string,
  orvalMode: OutputMode,
  operationName: string,
  exampleId: string,
): void {
  const file = exampleFileFor(example, orvalOutput, orvalMode);
  if (!file) {
    return;
  }
  const routeExampleValue = `${examplesDirectory(orvalOutput)} ${file.interfaceName} = ${file.literal}`;
  if (writtenRouteExampleValues.has(routeExampleValue)) {
    return;
  }
  const owner = `${orvalOutput} ${operationName}`;
  const write = (constantName: string): void => {
    const filePath = exampleFilePath(orvalOutput, constantName);
    writtenRouteExampleFiles.set(filePath, owner);
    writtenRouteExampleValues.add(routeExampleValue);
    createFileWithDirs(filePath, file.render(constantName, `${file.literal} as unknown as ${file.interfaceName}`));
  };

  const plainName = camelCase(file.interfaceName);
  const plainOwner = writtenRouteExampleFiles.get(exampleFilePath(orvalOutput, plainName));
  if (plainOwner === undefined) {
    write(plainName);
    return;
  }

  const operationScopedName = `${plainName}For${pascalCase(operationName)}`;
  if (!writtenRouteExampleFiles.has(exampleFilePath(orvalOutput, operationScopedName)) && plainOwner !== owner) {
    write(operationScopedName);
    return;
  }

  const exampleScopedName = `${operationScopedName}${pascalCase(exampleId)}`;
  if (writtenRouteExampleFiles.has(exampleFilePath(orvalOutput, exampleScopedName))) {
    throw new Error(`Two examples named ${exampleScopedName} have different values`);
  }
  write(exampleScopedName);
}

// Type and value of every schema example file of this process, keyed by its path.
const writtenSchemaExampleFiles = new Map<string, string>();

/**
 * Writes `<fileBaseName>.example.ts` with a constant typed as the schema, so the compiler checks the example.
 */
export function writeSchemaExampleFile(example: Example, orvalOutput: string, orvalMode: OutputMode, fileBaseName: string): void {
  const file = exampleFileFor(example, orvalOutput, orvalMode);
  if (!file) {
    return;
  }
  const filePath = exampleFilePath(orvalOutput, fileBaseName);
  const identity = `${file.interfaceName} = ${file.literal}`;
  const written = writtenSchemaExampleFiles.get(filePath);
  if (written === identity) {
    return;
  }
  if (written !== undefined) {
    throw new Error(`Two examples named ${fileBaseName} have different values`);
  }
  writtenSchemaExampleFiles.set(filePath, identity);
  createFileWithDirs(filePath, file.render(`${fileBaseName}: ${file.interfaceName}`, file.literal));
}

interface ExampleFile {
  interfaceName: string;
  literal: string;
  render: (declaration: string, initializer: string) => string;
}

function exampleFileFor(example: Example, orvalOutput: string, orvalMode: OutputMode): ExampleFile | undefined {
  const typeRef = example["x-type-ref"];
  if (!typeRef) {
    console.warn(`No "x-type-ref" found for example: ${JSON.stringify(example)}`);
    return undefined;
  }

  const lastTypeRefPart = typeRef.split("/").at(-1);
  if (!lastTypeRefPart) {
    throw new Error(`Cannot derive an interface name from "x-type-ref": ${typeRef}`);
  }
  const interfaceName = pascalCase(lastTypeRefPart);

  const schemasFileName = path.basename(orvalOutput);
  if (!schemasFileName) {
    throw new Error(`Cannot derive the schemas file name from the Orval output path: ${orvalOutput}`);
  }
  const schemasFileRelativePath =
    orvalMode === "split" ? `../${schemasFileName.replace(".ts", ".schemas")}` : `../${schemasFileName.replace(".ts", "")}`;

  return {
    interfaceName,
    literal: toTSObjectLiteral(example.value),
    render: (declaration, initializer) =>
      `import { type ${interfaceName} } from "${schemasFileRelativePath}";\n\nexport const ${declaration} = ${initializer};\n`,
  };
}

function exampleFilePath(orvalOutput: string, baseName: string): string {
  return path.join(examplesDirectory(orvalOutput), `${baseName}.example.ts`);
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
