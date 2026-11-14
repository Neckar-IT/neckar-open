/**
 * Input Transformer for Orval
 *
 * This transformer modifies the OpenAPI specification before Orval generates code.
 * It adds the x-source-kotlin-type and x-german-term extensions to schema descriptions so they appear
 * as JSDoc comments on generated TypeScript interfaces, and the x-enum-german-terms to the per-value
 * descriptions so they appear on the generated enum constants.
 */

import type { OpenApiDocument } from "@orval/core";

/**
 * Schema object with potential custom extensions
 */
interface SchemaWithExtensions {
  description?: string;
  "x-source-kotlin-type"?: string;
  "x-german-term"?: string;
  "x-enum-descriptions"?: Record<string, string>;
  "x-enum-german-terms"?: Record<string, string>;
  properties?: Record<string, SchemaWithExtensions>;
  items?: SchemaWithExtensions;
  allOf?: SchemaWithExtensions[];
  oneOf?: SchemaWithExtensions[];
  anyOf?: SchemaWithExtensions[];
}

/**
 * Transforms the OpenAPI specification to include x-source-kotlin-type and x-german-term
 * in schema descriptions for interface-level JSDoc generation, and x-enum-german-terms
 * in the enum value descriptions.
 *
 * @param spec - The OpenAPI specification object
 * @returns The transformed OpenAPI specification
 *
 * @example
 * // In orval.config.ts:
 * import { inputTransformer } from "@neckar-hub/orval-config";
 *
 * export default defineConfig({
 *   api: {
 *     input: {
 *       target: "./openapi.json",
 *       override: {
 *         transformer: inputTransformer,
 *       },
 *     },
 *   },
 * });
 */
export function inputTransformer(spec: OpenApiDocument): OpenApiDocument {
  const schemas = spec.components?.schemas;

  if (schemas == null) {
    return spec;
  }

  // Process each schema
  for (const schemaName of Object.keys(schemas)) {
    const schema = schemas[schemaName] as SchemaWithExtensions;
    addKotlinTypeToDescription(schema);
    addEnumGermanTermsToDescriptions(schema);
  }

  return spec;
}

/**
 * Adds the x-source-kotlin-type and the x-german-term to the description of a schema.
 * Recursively processes nested schemas.
 */
function addKotlinTypeToDescription(schema: SchemaWithExtensions | undefined): void {
  if (schema == null) {
    return;
  }

  const kotlinType = schema["x-source-kotlin-type"];
  const germanTerm = schema["x-german-term"];
  const typeDocs = [
    kotlinType ? `x-source-kotlin-type: ${kotlinType}` : undefined,
    germanTerm ? `x-german-term: ${germanTerm}` : undefined,
  ].filter((line) => line != null);

  if (typeDocs.length > 0) {
    const typeDoc = typeDocs.join("\n");

    if (schema.description) {
      // Prepend Kotlin type and German term to existing description
      schema.description = `${typeDoc}\n\n${schema.description}`;
    } else {
      schema.description = typeDoc;
    }
  }

  // Process nested schemas (but don't add Kotlin type to properties - jsDocFilter handles those)
  if (schema.allOf) {
    schema.allOf.forEach(addKotlinTypeToDescription);
  }
  if (schema.oneOf) {
    schema.oneOf.forEach(addKotlinTypeToDescription);
  }
  if (schema.anyOf) {
    schema.anyOf.forEach(addKotlinTypeToDescription);
  }
  if (schema.items) {
    addKotlinTypeToDescription(schema.items);
  }
}

/**
 * Adds the German term of each enum value to its x-enum-descriptions entry, which Orval writes as the JSDoc of the
 * generated enum constant.
 */
function addEnumGermanTermsToDescriptions(schema: SchemaWithExtensions): void {
  const germanTerms = schema["x-enum-german-terms"];

  if (germanTerms == null) {
    return;
  }

  const descriptions = { ...schema["x-enum-descriptions"] };
  for (const [value, germanTerm] of Object.entries(germanTerms)) {
    const description = descriptions[value];
    descriptions[value] = description ? `${description} (x-german-term: ${germanTerm})` : `x-german-term: ${germanTerm}`;
  }
  schema["x-enum-descriptions"] = descriptions;
}
