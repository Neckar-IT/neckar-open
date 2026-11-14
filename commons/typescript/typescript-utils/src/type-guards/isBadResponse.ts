import { isBadResponseType } from "../rest/RestResponse";

/**
 * Helper type to extract bad-response types from a response type.
 */
export type ExtractBadTypes<T> = T extends { type: infer U } ? (U extends string ? (U extends `bad/${string}` ? U : never) : never) : never;

/**
 * Type Guard to check if an object is a bad response (`"bad/<Subtype>"`)
 */
export function isBadResponse<T>(obj: unknown): obj is { type: ExtractBadTypes<T> } {
  return typeof obj === "object" && obj !== null && "type" in obj && typeof obj.type === "string" && isBadResponseType(obj.type);
}
