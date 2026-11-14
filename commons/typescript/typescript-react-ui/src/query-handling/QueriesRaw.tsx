/**
 * Imports
 */

import { type QueryResult } from "@neckar-hub/typescript-react-query";
import { type RestResponse } from "@neckar-hub/typescript-utils/rest";

/**
 * Base of the `Queries*` hierarchy: `QueriesRaw` → `QueriesResolved` → `QueriesData`.
 *
 * Passthrough component that always calls children with the raw query results.
 * Children receive `QueryResult[]` and are responsible for handling all states
 * (PENDING, SUCCESS, ERROR) themselves.
 *
 * Use this for table pages where the caller needs full control over loading states
 * (e.g., showing a skeleton instead of a loading indicator, or showing an isFetching
 * indicator during background refetch).
 *
 * Higher-level components `QueriesResolved` and `QueriesData` build on top of
 * `QueriesRaw`, so any future extension (dev validation, telemetry, logging) added
 * here automatically benefits the whole hierarchy.
 */
export function QueriesRaw<T extends readonly QueryResult<TResponse>[], TResponse extends RestResponse<unknown>>({
  queries,
  children,
}: {
  queries: [...T]; // Force tuple inference
  children: (queryResults: [...T]) => React.ReactElement;
}) {
  return children(queries);
}
