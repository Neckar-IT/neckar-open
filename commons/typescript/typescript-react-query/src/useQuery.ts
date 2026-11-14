/**
 * Imports
 */

import { useQuery as useQueryPrimitive, type QueryKey, type UseQueryOptions } from "@tanstack/react-query";
import { reactionToFailure } from "./reactionToFailure";

/**
 * Type Declarations
 */

type QueryMeta = UseQueryOptions["meta"];

/**
 * Status of a query observation.
 *
 * `status` + `isFetching` from `@tanstack/react-query` are folded into a single enum:
 * - `Pending` → no data yet (first load, parameter change, enabled=false)
 * - `Fetching` → data available, background refetch in progress (auto-reload, after invalidation)
 * - `Success` → data available, idle
 *
 * `Fetching` and `Success` both carry `response: TData`; they only differ in whether
 * a background request is currently running (used for header refetch indicators).
 *
 * There is no `Error` variant — `throwOnError` is configured globally on the
 * query client, so error cases never reach the result object. They are thrown and
 * caught by the surrounding error boundary. A same-origin 401 under
 * `unauthorizedEndsSession` keeps the last state: the auth provider replaces the page.
 * A query keeping its data through an outage (reaction `keepDataWithBanner`) reports `Success` or `Fetching`.
 */
export enum QueryStatus {
  Pending = "pending",
  Fetching = "fetching",
  Success = "success",
}

/**
 * Self-contained query result. Decoupled from `UseQueryResult` of `@tanstack/react-query`.
 *
 * Exactly four fields: `status`, `response`, `meta`, `queryKey`. Cheap to pass through,
 * trivial to mock. Consumers must not import React Query types to work with this result.
 *
 * `queryKey` is carried so downstream consumers (e.g. mutations, pages) can invalidate
 * the query via `queryClient.invalidateQueries({ queryKey })` without having to re-derive
 * the key at the call site.
 */
export type QueryResult<TData> =
  | {
      status: QueryStatus.Pending;
      response: undefined;
      meta: QueryMeta | undefined;
      queryKey: QueryKey;
    }
  | {
      status: QueryStatus.Fetching;
      response: TData;
      meta: QueryMeta | undefined;
      queryKey: QueryKey;
    }
  | {
      status: QueryStatus.Success;
      response: TData;
      meta: QueryMeta | undefined;
      queryKey: QueryKey;
    };

export type QueryResultSuccess<TData> = Extract<QueryResult<TData>, { status: QueryStatus.Fetching | QueryStatus.Success }>;

/**
 * Customized useQuery Hook
 *
 * Wraps `@tanstack/react-query`'s `useQuery` and folds `status` + `isFetching` into our
 * three-state `QueryStatus` enum. Renames `data` to `response` so it is easier to
 * distinguish the query-library state from our own response envelope.
 */
export function useQuery<TData>(queryOptions: UseQueryOptions<TData>): QueryResult<TData> {
  const { status, data, error, isFetching } = useQueryPrimitive<TData>(queryOptions);
  const meta = queryOptions.meta;
  const queryKey = queryOptions.queryKey;

  switch (status) {
    case "pending":
      return {
        status: QueryStatus.Pending,
        response: undefined,
        meta,
        queryKey,
      };

    case "error": {
      const reaction = reactionToFailure(error, data === undefined ? "query" : "queryWithData");
      if (reaction.type === "sessionExpired") {
        // Reached when throwOnError lets the 401 through, as unauthorizedEndsSession does; the auth provider replaces the page.
        return data === undefined
          ? { status: QueryStatus.Pending, response: undefined, meta, queryKey }
          : { status: QueryStatus.Success, response: data, meta, queryKey };
      }
      if (reaction.type === "keepDataWithBanner" && data !== undefined) {
        return { status: isFetching ? QueryStatus.Fetching : QueryStatus.Success, response: data, meta, queryKey };
      }
      throw new Error(
        "useQuery received 'error' status from @tanstack/react-query. This is unreachable with throwOnError configured globally on the query client.",
      );
    }

    case "success":
      return {
        status: isFetching ? QueryStatus.Fetching : QueryStatus.Success,
        response: data,
        meta,
        queryKey,
      };
    default:
      throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(status)}`);
  }
}
