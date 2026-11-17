/**
 * Imports
 */

import {
  type ExpectedApplicationErrorWithType,
  ExpectedApplicationErrorWithTypeType,
  type UnexpectedApplicationErrorWithType,
  UnexpectedApplicationErrorWithTypeType,
} from "../../build/generated/open-api/openapi-types.schemas";

/**
 * Type Guards
 */

export function isExpectedApplicationError(data: unknown): data is ExpectedApplicationErrorWithType {
  return discriminatorOf(data) === ExpectedApplicationErrorWithTypeType.ExpectedError;
}

export function isUnexpectedApplicationError(data: unknown): data is UnexpectedApplicationErrorWithType {
  return discriminatorOf(data) === UnexpectedApplicationErrorWithTypeType.UnexpectedError;
}

/**
 * Reads the `type` discriminator without assuming an object. Callers pass raw response bodies —
 * `error.response?.data` is `undefined` on a network failure and `null` on an empty JSON body,
 * and a plain property read on either throws inside the global error handler.
 */
function discriminatorOf(data: unknown): unknown {
  if (typeof data !== "object" || data === null) {
    return undefined;
  }

  return (data as { type?: unknown }).type;
}
