/**
 * Imports
 */

import { isSameOriginUnauthorized, useQueryClient } from "@neckar-hub/typescript-react-query";
import { useEffect, useState } from "react";

/**
 * Turns `true` once any query or mutation of the client fails with a same-origin HTTP 401, and stays
 * `true`: the session behind the page ended, and only a new sign-in brings it back.
 */
export function useSameOriginUnauthorized(): boolean {
  const queryClient = useQueryClient();
  const [unauthorized, setUnauthorized] = useState(false);

  useEffect(() => {
    const onError = (error: unknown): void => {
      if (isSameOriginUnauthorized(error)) {
        setUnauthorized(true);
      }
    };

    const unsubscribeQueries = queryClient.getQueryCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "error") {
        onError(event.action.error);
      }
    });
    const unsubscribeMutations = queryClient.getMutationCache().subscribe((event) => {
      if (event.type === "updated" && event.action.type === "error") {
        onError(event.action.error);
      }
    });

    return () => {
      unsubscribeQueries();
      unsubscribeMutations();
    };
  }, [queryClient]);

  return unauthorized;
}
