import type { QueryResult } from "@neckar-hub/typescript-react-query";
import { dataIfSuccess, type RestResponse } from "@neckar-hub/typescript-utils/rest";
import type { Uuid } from "@neckar-hub/typescript-utils/types";

/**
 * Returns `null` for an absent id or a bad response, `undefined` while pending, the data otherwise. Pass the query
 * called with the same id: `resolvedById(authorId, useAuthorByIdQuery(authorId))`.
 */
export function resolvedById<T>(id: Uuid | null | undefined, query: QueryResult<RestResponse<T>>): T | null | undefined {
  return id == null ? null : dataIfSuccess(query.response);
}
