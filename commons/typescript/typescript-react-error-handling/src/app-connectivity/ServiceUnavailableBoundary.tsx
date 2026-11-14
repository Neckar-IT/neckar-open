import { isServiceUnavailable, type Query, type QueryClient, useQueryClient } from "@neckar-hub/typescript-react-query";
import { useEffect } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { serviceUnavailableErrorHandler } from "../global-error-handler/default-error-handlers/serviceUnavailableErrorHandler";
import { isHealthCheckQuery } from "./appConnectivityState";

/**
 * Renders `ServiceUnavailablePage` for an error of an unavailable service and hands every other error to the boundary above.
 * Sits below the health poll, so `ServiceUnavailablePage` shows the polled state and recovers without a page reload.
 */
export function ServiceUnavailableBoundary(props: { children: React.ReactNode }): React.ReactElement {
  const queryClient = useQueryClient();

  return (
    <ErrorBoundary
      fallbackRender={({ error, resetErrorBoundary }) => {
        const recover = () => {
          void resetUnavailableQueries(queryClient);
          resetErrorBoundary();
        };
        const page = serviceUnavailableErrorHandler(error, recover);
        if (page === null) {
          throw error;
        }
        return <ResetOnHistoryChange reset={recover}>{page}</ResetOnHistoryChange>;
      }}
    >
      {props.children}
    </ErrorBoundary>
  );
}

/**
 * Fetches every query again that failed for an unavailable service. A query without data is reset, so its next observer
 * fetches; a query that kept its data through the outage refetches and keeps it on screen.
 */
export async function resetUnavailableQueries(queryClient: QueryClient): Promise<void> {
  const failed = (query: Query) => isServiceUnavailable(query.state.error) && !isHealthCheckQuery(query.queryKey);
  await Promise.all([
    queryClient.resetQueries({ predicate: (query) => failed(query) && query.state.data === undefined }),
    // A query with data keeps the outage error while it refetches; the next round lets that slow answer through.
    queryClient.invalidateQueries({ predicate: (query) => failed(query) && query.state.data !== undefined }, { cancelRefetch: false }),
  ]);
}

/**
 * The back button of `ServiceUnavailablePage` changes the history entry; the page below it renders anew.
 */
function ResetOnHistoryChange(props: { reset: () => void; children: React.ReactNode }): React.ReactNode {
  const { reset } = props;
  useEffect(() => {
    window.addEventListener("popstate", reset);
    return () => window.removeEventListener("popstate", reset);
  }, [reset]);

  return props.children;
}
