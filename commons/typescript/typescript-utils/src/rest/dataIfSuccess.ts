import { isBadResponseType, unexpectedRestResponseTypeError, type RestResponse } from "./RestResponse";

/**
 * Extracts the data payload from a {@link RestResponse}, distinguishing three
 * states for downstream renderers:
 *
 * | Response state                      | Returns     | Meaning                              |
 * |-------------------------------------|-------------|--------------------------------------|
 * | `undefined` (query pending)         | `undefined` | not yet loaded — Skeleton-eligible   |
 * | `{ type: "bad/..." }` (failure)     | `null`      | loaded, absent — NoValue-eligible    |
 * | `{ type: "good", data }` (success)  | `data`      | loaded, present — render             |
 *
 * Entity property renderers honor the same distinction: `undefined` → Skeleton,
 * `null` → NoValue. Passing this function's result straight into an entity
 * renderer therefore renders the correct affordance for each state without any
 * branching at the call site.
 */
export function dataIfSuccess<T>(response: RestResponse<T> | undefined): T | null | undefined {
  if (response === undefined) {
    return undefined;
  }
  if (response.type === "good") {
    return response.data;
  }
  if (isBadResponseType(response.type)) {
    return null;
  }
  throw unexpectedRestResponseTypeError(response.type);
}
