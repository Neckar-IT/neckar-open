/**
 * Imports
 */

import { type PaginationInfo } from "../../build/generated/open-api/openapi-types.schemas";

/**
 * A response from our Kotlin REST APIs if the query produced a valid business result.
 */
export interface RestResponseGood<T> {
  type: "good";
  data: T;
  paginationInfo?: PaginationInfo;
}

/**
 * A response from our Kotlin REST APIs if the query did not produce a valid business result.
 *
 * The discriminator is always a `"bad/<Subtype>"` value. For example, if an entity could not
 * be found in the database: { type: "bad/NotFound" }
 */
export interface RestResponseBad {
  type: `bad/${string}`;
}

/**
 * Every response from the Kotlin REST APIs has a `type` property to indicate whether the request
 * produced a good (successful) result or a bad (expected business-level) one.
 *
 * The contract allows exactly two discriminator shapes: `"good"` and `"bad/<Subtype>"`
 * (e.g. "bad/NotFound"). Plain `"bad"` is forbidden.
 */
export type RestResponse<T> = RestResponseGood<T> | RestResponseBad;

/**
 * Helper constant that can be used to manually declare a response as good (e.g. in tests)
 */
export const restResponseGoodType = "good" as const;

/**
 * Returns true if the discriminator marks a bad response, i.e. is a `"bad/<Subtype>"` value.
 */
export function isBadResponseType(type: string): type is RestResponseBad["type"] {
  return type.startsWith("bad/");
}

/**
 * Builds the error thrown when a response discriminator violates the RestResponse contract.
 *
 * The discriminator originates from the `@SerialName` annotation on the Kotlin response variant,
 * so the fix is always on the Kotlin side.
 */
export function unexpectedRestResponseTypeError(type: string): Error {
  return new Error(
    `Unexpected RestResponse discriminator "${type}". ` +
      `The RestResponse contract requires every variant to declare "good" (GoodResponse) ` +
      `or "bad/<Subtype>" (BadResponse), e.g. "bad/NotFound". Plain "bad" is forbidden. ` +
      `The discriminator comes from the @SerialName annotation on the Kotlin response variant - ` +
      `fix it there, e.g. @SerialName("bad/NotFound").`,
  );
}
