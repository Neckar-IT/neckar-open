// @ts-check

/**
 * Custom oxlint plugin for the neckar.it monorepo.
 *
 * Plugin is shipped as `.mjs` because oxlint loads JS plugins through Node's
 * native ESM loader, which has no TypeScript support. Keep this file small and
 * dependency-free; if it grows, switch to a tsc build step that emits `.js`.
 */

/**
 * @typedef {{ type: string, [key: string]: any }} AstNode
 * @typedef {{ report: (d: { node: AstNode, message: string }) => void }} RuleContext
 */

const ID_SUFFIX = /[a-z0-9](Id|Ids|Uuid|Uuids|Ref|Refs)$/;

/**
 * @param {AstNode | undefined | null} typeNode
 * @returns {boolean}
 */
function isUuidType(typeNode) {
  if (!typeNode) return false;

  if (typeNode.type === "TSTypeReference" && isIdentifierNamed(typeNode.typeName, "Uuid")) {
    return true;
  }

  if (typeNode.type === "TSArrayType") {
    return isUuidType(typeNode.elementType);
  }

  if (typeNode.type === "TSTypeReference" && isIdentifierNamed(typeNode.typeName, "Array", "ReadonlyArray")) {
    const params = typeNode.typeArguments?.params;
    return params?.length === 1 && isUuidType(params[0]);
  }

  if (typeNode.type === "TSUnionType") {
    const members = typeNode.types ?? [];
    const meaningful = members.filter(
      /** @param {AstNode} t */
      (t) => t.type !== "TSNullKeyword" && t.type !== "TSUndefinedKeyword",
    );
    return meaningful.length > 0 && meaningful.every(isUuidType);
  }

  // Indexed access on a known id-shaped key — `Foo["id"]`, `Foo["uuid"]` —
  // resolves to the brand under TS's checker; we accept it without resolving.
  if (typeNode.type === "TSIndexedAccessType") {
    const indexType = typeNode.indexType;
    if (
      indexType?.type === "TSLiteralType" &&
      indexType.literal?.type === "Literal" &&
      typeof indexType.literal.value === "string" &&
      ["id", "uuid"].includes(indexType.literal.value.toLowerCase())
    ) {
      return true;
    }
  }

  return false;
}

/**
 * @param {AstNode | undefined | null} node
 * @param {...string} names
 */
function isIdentifierNamed(node, ...names) {
  if (!node || node.type !== "Identifier") return false;
  return typeof node.name === "string" && names.includes(node.name);
}

/**
 * @param {AstNode | undefined | null} keyNode
 * @returns {string | null}
 */
function propertyName(keyNode) {
  if (!keyNode) return null;
  // Only Identifier keys count. String-literal keys (e.g. URL-pattern routing
  // tables `"/users/:userId": ...`) are not real ID containers.
  if (keyNode.type === "Identifier") return typeof keyNode.name === "string" ? keyNode.name : null;
  return null;
}

/**
 * @param {AstNode} node
 * @param {RuleContext} context
 */
function check(node, context) {
  const name = propertyName(node.key);
  if (name === null) return;
  if (!ID_SUFFIX.test(name)) return;

  const typeAnnotation = node.typeAnnotation?.typeAnnotation;
  if (!typeAnnotation) return;

  // Function-typed properties (setters, callbacks) carry no ID value themselves
  // — the inner parameter types are checked separately by their own properties.
  if (typeAnnotation.type === "TSFunctionType") return;

  if (isUuidType(typeAnnotation)) return;

  context.report({
    node,
    message: `Property '${name}' must be typed as 'Uuid' (the brand from @neckar-hub/typescript-utils), not a raw type. Allowed: Uuid, Uuid[], Array<Uuid>, Uuid | null, Uuid | undefined, Foo["id"], Foo["uuid"].`,
  });
}

const noRawIdTypes = {
  meta: {
    type: "problem",
    docs: {
      description: "Forbid raw types for ID-shaped properties; require the Uuid brand instead.",
    },
  },
  /** @param {RuleContext} context */
  create(context) {
    return {
      /** @param {AstNode} node */
      TSPropertySignature(node) {
        check(node, context);
      },
      /** @param {AstNode} node */
      PropertyDefinition(node) {
        check(node, context);
      },
    };
  },
};

/**
 * Zod methods whose params object — the one carrying `error` / the deprecated
 * `message` — is the FIRST argument: checks without a value (`.positive()`) and
 * the bare type constructors (`z.string()`).
 */
const ZOD_PARAMS_FIRST_ARGUMENT = new Set([
  "bigint",
  "boolean",
  "date",
  "email",
  "file",
  "int",
  "negative",
  "nonempty",
  "nonnegative",
  "nonpositive",
  "number",
  "positive",
  "string",
  "url",
  "uuid",
]);

/**
 * Zod methods whose params object follows the value argument — `.min(1, params)`,
 * `.refine(check, params)`, `z.enum(values, params)`. The first argument is the
 * value/shape and must never be treated as a params object: `z.object({ message:
 * z.string() })` declares a field called `message`, it does not set an error text.
 */
const ZOD_PARAMS_LATER_ARGUMENT = new Set([
  "array",
  "custom",
  "endsWith",
  "enum",
  "gt",
  "gte",
  "instanceof",
  "length",
  "literal",
  "lt",
  "lte",
  "max",
  "min",
  "multipleOf",
  "nativeEnum",
  "object",
  "record",
  "refine",
  "regex",
  "startsWith",
  "superRefine",
  "tuple",
  "union",
]);

/**
 * @param {AstNode} callExpression
 * @returns {string | null}
 */
function calleeMethodName(callExpression) {
  const callee = callExpression.callee;
  if (!callee || callee.type !== "MemberExpression" || callee.computed === true) return null;
  return propertyName(callee.property);
}

/**
 * @param {AstNode} objectExpression
 * @returns {AstNode | null}
 */
function findMessageProperty(objectExpression) {
  for (const property of objectExpression.properties ?? []) {
    if (property.type !== "Property" || property.computed === true) continue;
    if (propertyName(property.key) === "message") return property;
  }
  return null;
}

const noDeprecatedZodMessage = {
  meta: {
    type: "problem",
    docs: {
      description: "Forbid the deprecated `message` parameter of Zod schemas and checks; require `error` instead.",
    },
  },
  /** @param {RuleContext} context */
  create(context) {
    return {
      /** @param {AstNode} node */
      CallExpression(node) {
        const methodName = calleeMethodName(node);
        if (methodName === null) return;

        const firstParamsIndex = ZOD_PARAMS_FIRST_ARGUMENT.has(methodName) ? 0 : ZOD_PARAMS_LATER_ARGUMENT.has(methodName) ? 1 : null;
        if (firstParamsIndex === null) return;

        for (const argument of (node.arguments ?? []).slice(firstParamsIndex)) {
          if (argument.type !== "ObjectExpression") continue;

          const messageProperty = findMessageProperty(argument);
          if (messageProperty === null) continue;

          context.report({
            node: messageProperty,
            message: `Zod deprecated the 'message' parameter of '${methodName}'. Use 'error' instead — same value, same position.`,
          });
        }
      },
    };
  },
};

export default {
  meta: {
    name: "neckar",
  },
  rules: {
    "no-raw-id-types": noRawIdTypes,
    "no-deprecated-zod-message": noDeprecatedZodMessage,
  },
};
