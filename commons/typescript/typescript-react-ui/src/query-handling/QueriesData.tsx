/**
 * Imports
 */

import { type QueryResult } from "@neckar-hub/typescript-react-query";
import { type RestResponse } from "@neckar-hub/typescript-utils/rest";
import { BadResponseFallback } from "./BadResponseFallback";
import { QueriesResolved } from "./QueriesResolved";
import { requireGoodResponse } from "./requireGoodResponse";

/**
 * Helper type to extract the success data type from each element of the query results tuple.
 */
type ExtractSuccessResponseData<T extends readonly QueryResult<TResponse>[], TResponse extends RestResponse<unknown>> = {
  [K in keyof T]: T[K] extends QueryResult<RestResponse<infer TData>> ? TData : never;
};

/**
 * Top layer of the `Queries*` hierarchy: builds on `QueriesResolved`, which in turn
 * builds on `QueriesRaw`. Any behavior added to `QueriesRaw` (dev validation, telemetry,
 * logging) propagates transitively through this component.
 *
 * Blocks on Pending, handles failure responses, and passes only the success data to children.
 *
 * - Pending: renders `<LoadingIndicator />` (via QueriesResolved)
 * - Fetching/Success + bad/NotFound: renders `<BadResponseFallback />`
 * - Fetching/Success + other bad response: throws (caught by ErrorBoundary)
 * - Fetching/Success + good: calls children with `T[]` (extracted data)
 *
 * This is the simplest abstraction level — children only receive the data,
 * no query metadata.
 */
export function QueriesData<T extends readonly QueryResult<TResponse>[], TResponse extends RestResponse<unknown>>({
  queries,
  children,
}: {
  queries: [...T]; // Force tuple inference
  children: (data: ExtractSuccessResponseData<T, TResponse>) => React.ReactElement;
}) {
  return (
    <QueriesResolved<T, TResponse> queries={queries}>
      {(queryResultsSuccess) => {
        const data = [];
        for (const result of queryResultsSuccess) {
          try {
            requireGoodResponse(result);
          } catch (error: unknown) {
            return <BadResponseFallback error={error} />;
          }
          data.push(result.response.data);
        }

        return children(data as ExtractSuccessResponseData<T, TResponse>);
      }}
    </QueriesResolved>
  );
}
