/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */

/**
 * Custom Orval Transformer
 *
 * This function is used convert our custom OpenApi- Extensions to comments in the generated typescript code
 */

/**
 * Imports
 */

import { type GeneratorVerbOptions, type OpenApiNonBooleanSchemaObject, type OpenApiParameterObject, type OpenApiReferenceObject, type OpenApiSchemaObject } from "@orval/core";

/**
 * Type Declarations
 */

interface XSourceLocation {
  location: string;
  className: string;
  methodName: string;
}

interface WithKotlinType {
  "x-source-kotlin-type"?: unknown;
}

/**
 * Transformer Function
 */

export function customTransformer(data: GeneratorVerbOptions) {
  const summary = data.originalOperation.summary;
  const xSourceLocation = data.originalOperation["x-source-location"] as XSourceLocation | undefined;
  const parameters = data.originalOperation.parameters;
  const response200 = data.response.originalSchema?.["200"];
  const mediaTypeSchema = response200 && "content" in response200 ? response200.content?.["application/json"]?.schema : undefined;
  const kotlinType = objectSchema(mediaTypeSchema)?.["x-source-kotlin-type"] as string | undefined;

  data.doc = "/**\n";

  if (summary) data.doc += ` * ${summary}\n *\n`;

  if (parameters && parameters.length > 0) {
    parameters
      .filter((parameter) => isParameterObject(parameter))
      .filter((parameter) => parameter.in === "path")
      .forEach((parameter) => {
        data.doc += paramTag(parameter, lowercaseFirstLetter(parameter.name), data.operationId);
      });

    parameters
      .filter((parameter) => isParameterObject(parameter))
      .filter((parameter) => parameter.in === "query")
      .forEach((parameter, index) => {
        if (index === 0) data.doc += " * @param {Object} query\n";

        data.doc += paramTag(parameter, `query.${parameter.name}`, data.operationId);
      });
  }

  if (kotlinType) data.doc += ` * @returns ${kotlinType}\n`;

  data.doc += " */\n";

  if (xSourceLocation) {
    data.doc += "\n // Kotlin Source:\n";
    data.doc += ` //   Location:   ${xSourceLocation.location}\n`;
    data.doc += ` //   Classname:  ${xSourceLocation.className}\n`;
    data.doc += ` //   Methodname: ${xSourceLocation.methodName}\n\n`;
  }

  return data;
}

/**
 * Parameter Documentation
 */

/**
 * Renders one `@param` tag.
 *
 * The type and the Kotlin type are both optional in the specification. Whichever one is missing is
 * left out of the tag: rendering it as the literal text `undefined` put the gap into the generated
 * sources as if it were the type — `@param {undefined} query.sortBy (undefined)`.
 */
function paramTag(parameter: ResolvedParameterObject, parameterName: string, operationId: string): string {
  const type = parameterType(parameter, operationId);
  const kotlinType = parameterKotlinType(parameter);

  const renderedType = type === undefined ? "" : `{${JSON.stringify(type)}} `;
  const renderedKotlinType = kotlinType === undefined ? "" : ` (${kotlinType})`;

  return ` * @param ${renderedType}${parameterName}${renderedKotlinType}\n`;
}

/**
 * The type of a parameter as the specification states it.
 *
 * Orval hands the transformer the *unresolved* parameter schema, so a parameter that points at a
 * named schema arrives as `{ "$ref": "#/components/schemas/ArchiveFilter" }` and carries no `type` —
 * the schema name is what documents it. A schema with neither is a gap in the specification and is
 * reported rather than silently documented as a parameter of an unnamed type.
 */
function parameterType(parameter: ResolvedParameterObject, operationId: string): unknown {
  const schema = objectSchema("schema" in parameter ? parameter.schema : undefined);

  const reference = schema?.$ref;
  if (typeof reference === "string") {
    const schemaName = reference.split("/").at(-1);
    if (schemaName) return schemaName;
  }

  const type = schema?.type;
  if (type !== undefined) return type;

  console.warn(`No "type" and no "$ref" for parameter "${parameter.name}" of operation "${operationId}", schema: ${JSON.stringify(schema)}`);
  return undefined;
}

/**
 * The Kotlin type of a parameter.
 *
 * The Ktor side writes `x-source-kotlin-type` onto the parameter schema, and for a parameter that
 * points at a named schema onto the parameter itself — the shared schema cannot carry the Kotlin
 * type of one particular use of it.
 */
function parameterKotlinType(parameter: ResolvedParameterObject): string | undefined {
  const schema = objectSchema("schema" in parameter ? parameter.schema : undefined);
  const onSchema = schema?.["x-source-kotlin-type"];
  if (typeof onSchema === "string") return onSchema;

  const onParameter = (parameter as WithKotlinType)["x-source-kotlin-type"];
  return typeof onParameter === "string" ? onParameter : undefined;
}

/**
 * Narrows a schema to its non-boolean shape; a JSON Schema `true`/`false` literal carries
 * neither `x-*` extensions nor `type`, so callers only need the object shape.
 */
function objectSchema(schema: OpenApiSchemaObject | undefined): OpenApiNonBooleanSchemaObject | undefined {
  return typeof schema === "object" ? schema : undefined;
}

/**
 * Parameter Type Guard
 */

type ResolvedParameterObject = OpenApiParameterObject & { name: string; in: string };

function isParameterObject(obj: OpenApiParameterObject | OpenApiReferenceObject): obj is ResolvedParameterObject {
  return "name" in obj;
}

/**
 * lowercase first letter
 */

function lowercaseFirstLetter(str: string) {
  return str.charAt(0).toLowerCase() + str.slice(1);
}
