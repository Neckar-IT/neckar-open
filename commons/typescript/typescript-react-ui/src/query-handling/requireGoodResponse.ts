import { type QueryResultSuccess } from "@neckar-hub/typescript-react-query";
import {
  isBadResponseType,
  unexpectedRestResponseTypeError,
  type RestResponse,
  type RestResponseGood,
} from "@neckar-hub/typescript-utils/rest";
import { BadResponseError } from "./BadResponseError";

/**
 * Type guard that asserts the response is a `GoodResponse` (business-level success)
 * with generic data type `T`.
 *
 * Always use this function inside a try/catch block with a `BadResponseFallback`:
 * ```
 * try {
 *   requireGoodResponse(queryResultSuccess);
 *   return <PureComponent data={queryResultSuccess.response.data} />
 * } catch (error) {
 *   return <BadResponseFallback error={error} />
 * }
 * ```
 *
 * TODO: This could be improved by a function that triggers a rerender on async errors and uses
 * the React Router's `errorElement` as a fallback.
 */
export function requireGoodResponse<T>(
  result: QueryResultSuccess<RestResponse<T>>,
  message?: string,
): asserts result is QueryResultSuccess<RestResponseGood<T>> {
  if (result.response.type === "good") {
    return;
  }
  if (isBadResponseType(result.response.type)) {
    throw new BadResponseError({
      badType: result.response.type,
      message,
      entityLabel: result.meta?.entityLabel,
    });
  }
  throw unexpectedRestResponseTypeError(result.response.type);
}
