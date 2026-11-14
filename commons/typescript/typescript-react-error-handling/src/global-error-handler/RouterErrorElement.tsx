/**
 * Imports
 */

import { QueryClientContext } from "@neckar-hub/typescript-react-query";
import { useCallback, useContext, useEffect, useRef } from "react";
import { useLocation, useNavigate, useRevalidator, useRouteError } from "react-router";
import { resetUnavailableQueries } from "../app-connectivity/ServiceUnavailableBoundary";
import { type ErrorHandler, GlobalErrorFallback } from "./GlobalErrorFallback";

/**
 * Prop Types
 */

interface RouterErrorElementProps {
  errorHandlers?: ErrorHandler[];
}

/**
 * Router Error Element
 *
 * When using `createBrowserRouter` from `react-router`, all rendering errors and errors inside loaders and actions
 * are caught by React Router and will not bubble up to a global error boundary. Unfortunately there is no way to
 * disable this behavior: https://github.com/remix-run/react-router/discussions/10166#discussioncomment-5985748
 *
 * In order to use our custom error handling, we need to pass this component as the error element to the topmost route
 * in `createBrowserRouter`. A `<BrowserRouter>` with `<Routes>` ignores `errorElement`; its errors reach the error boundaries.
 */

export function RouterErrorElement(props: RouterErrorElementProps) {
  const error = useRouteError();
  const location = useLocation();
  const navigate = useNavigate();
  const { revalidate } = useRevalidator();
  // An app without react-query has no failed query to reset.
  const queryClient = useContext(QueryClientContext);
  // The location the page shows now; undefined once the page is gone, as after the user went back during the reset.
  const shownLocationKey = useRef<string | undefined>(location.key);
  useEffect(() => {
    shownLocationKey.current = location.key;
    return () => {
      shownLocationKey.current = undefined;
    };
  }, [location.key]);

  // A failed query refetches on mount only after a reset. The navigation to the same location clears a render error,
  // the revalidation a loader error, which a navigation keeping the hash leaves in place.
  const recover = useCallback(() => {
    const failedLocation = location;
    const resetAndReload = async () => {
      if (queryClient !== undefined) {
        await resetUnavailableQueries(queryClient);
      }
      if (shownLocationKey.current !== failedLocation.key) {
        return;
      }
      await navigate(
        { pathname: failedLocation.pathname, search: failedLocation.search, hash: failedLocation.hash },
        { replace: true, state: failedLocation.state as unknown },
      );
      await revalidate();
    };
    void resetAndReload();
  }, [queryClient, navigate, revalidate, location]);

  return <GlobalErrorFallback error={error} errorCaughtBy="router-error-element" errorHandlers={props.errorHandlers} recover={recover} />;
}
