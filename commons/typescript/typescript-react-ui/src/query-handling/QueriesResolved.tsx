/**
 * Imports
 */

import { QueryStatus, type QueryResult, type QueryResultSuccess } from "@neckar-hub/typescript-react-query";
import { type RestResponse } from "@neckar-hub/typescript-utils/rest";
import { LoadingIndicator } from "../components/feedback/loading-indicator";
import { QueriesRaw } from "./QueriesRaw";

/**
 * Helper type to extract QueryResultSuccess from each element of the query results tuple.
 */
type ExtractQueryResultSuccess<T extends readonly QueryResult<unknown>[]> = {
  [K in keyof T]: T[K] extends QueryResult<infer TResponse> ? QueryResultSuccess<TResponse> : never;
};

/**
 * Middle layer of the `Queries*` hierarchy: built on top of `QueriesRaw`, consumed by `QueriesData`.
 *
 * Blocks on Pending and passes Success/Fetching results to children.
 *
 * - Pending: renders `<LoadingIndicator />`
 * - Fetching / Success: calls children with `QueryResultSuccess[]`
 *
 * `QueryStatus` has no error variant — `throwOnError` is configured globally, so
 * error cases never reach this component.
 *
 * Children receive the full `QueryResultSuccess` including `response`, allowing custom
 * failure handling (e.g., domain-specific NotFound pages) and background refetch
 * indicators (via `status === QueryStatus.Fetching`).
 *
 * Internally delegates to `QueriesRaw`, so the hierarchy Raw → Resolved → Data is
 * visible in the call graph, not just in documentation.
 */
export function QueriesResolved<T extends readonly QueryResult<TResponse>[], TResponse extends RestResponse<unknown>>({
  queries,
  children,
}: {
  queries: [...T]; // Force tuple inference
  children: (queryResultsSuccess: ExtractQueryResultSuccess<T>) => React.ReactElement;
}) {
  return (
    <QueriesRaw<T, TResponse> queries={queries}>
      {(rawQueries) => {
        if (rawQueries.some((query) => query.status === QueryStatus.Pending)) {
          return <LoadingIndicator />;
        }

        return children(rawQueries as ExtractQueryResultSuccess<T>);
      }}
    </QueriesRaw>
  );
}
