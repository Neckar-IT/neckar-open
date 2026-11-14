/**
 * JSDoc Filter for Orval Schema Types
 *
 * This filter function configures which OpenAPI schema properties get converted
 * to JSDoc comments in the generated TypeScript types.
 */

/**
 * Schema property entry for JSDoc generation
 */
interface JsDocEntry {
  key: string;
  value: string;
}

/**
 * OpenAPI schema object that may contain properties for JSDoc generation
 */
type SchemaObject = Record<string, unknown> | undefined | null;

/**
 * Allowlist of OpenAPI schema properties that should be included in JSDoc comments.
 * These properties provide useful documentation for TypeScript types.
 */
const JsDocAllowlist = [
  // Description and documentation
  "description",
  "title",
  "summary",

  // Type information (not "type" - redundant with TypeScript types)
  "format",
  "nullable",

  // String constraints
  "maxLength",
  "minLength",
  "pattern",

  // Number constraints
  "minimum",
  "maximum",
  "exclusiveMinimum",
  "exclusiveMaximum",
  "multipleOf",

  // Array constraints
  "minItems",
  "maxItems",
  "uniqueItems",

  // Enum values
  "enum",

  // Default value
  "default",

  // Deprecation
  "deprecated",
  "x-deprecated-reason",

  // Examples
  "example",

  // Custom extensions for Kotlin source info
  "x-source-kotlin-type",
  "x-source-location",

  // Unit information for value classes
  "x-unit",
  "x-unit-symbol",
  "x-unit-name",

  // German term for i18n
  "x-german-term",
] as const;

/**
 * Filter function for Orval's jsDoc configuration.
 *
 * This function determines which schema properties from OpenAPI specs are included
 * in the generated JSDoc comments for TypeScript types and interfaces.
 *
 * @param schema - The OpenAPI schema object to extract JSDoc properties from
 * @returns An array of key-value pairs to include in the JSDoc comment
 *
 * @example
 * // In orval.config.ts:
 * import { jsDocFilter } from "@neckar-hub/orval-config";
 *
 * export default defineConfig({
 *   api: {
 *     output: {
 *       override: {
 *         jsDoc: {
 *           filter: jsDocFilter,
 *         },
 *       },
 *     },
 *   },
 * });
 */
export function jsDocFilter(schema: SchemaObject): JsDocEntry[] {
  if (schema == null) {
    return [];
  }

  return Object.entries(schema)
    .filter(([key]) => JsDocAllowlist.includes(key as (typeof JsDocAllowlist)[number]))
    .map(([key, value]) => ({
      key,
      value: stringifyValue(value),
    }))
    .toSorted((a, b) => {
      // Sort by priority: description first, then type info, then constraints
      const priority = getPriority(a.key) - getPriority(b.key);
      if (priority !== 0) return priority;
      // Secondary sort by key name length
      return a.key.length - b.key.length;
    });
}

/**
 * Converts a schema value to a string representation for JSDoc.
 */
function stringifyValue(value: unknown): string {
  if (value === null) {
    return "null";
  }
  if (value === undefined) {
    return "undefined";
  }
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "boolean" || typeof value === "number" || typeof value === "bigint") {
    return String(value);
  }
  if (Array.isArray(value)) {
    return value.map((v) => stringifyValue(v)).join(", ");
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  // Fallback for symbol, function, or other exotic types
  return typeof value === "function" ? "[function]" : String(value as string | number);
}

/**
 * Returns priority for sorting JSDoc entries.
 * Lower numbers appear first.
 */
function getPriority(key: string): number {
  switch (key) {
    case "description":
    case "title":
    case "summary":
      return 0; // Documentation first
    case "deprecated":
      return 1; // Important warning
    case "type":
    case "format":
    case "nullable":
      return 2; // Type information
    case "x-source-kotlin-type":
    case "x-source-location":
      return 3; // Source info
    case "x-unit":
    case "x-unit-symbol":
    case "x-unit-name":
    case "x-german-term":
      return 4; // Unit and i18n info
    case "default":
    case "example":
      return 5; // Examples
    case "enum":
      return 6; // Enumeration
    default:
      return 7; // Constraints and other properties
  }
}
