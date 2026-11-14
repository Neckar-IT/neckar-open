/**
 * Imports
 */

import { useIsMounted } from "@neckar-hub/typescript-react-utils/utility-hooks";
import { isPromiseLike } from "@neckar-hub/typescript-utils/promise-utils";
import { useCallback, useState } from "react";

/**
 * Hook to handle pending state for asynchronous actions
 */

export function usePendingAction<Args extends unknown[], R>(fn?: (...args: Args) => R | Promise<R>, opts?: { externalPending?: boolean }) {
  const [internalPending, setInternalPending] = useState(false);
  const isMounted = useIsMounted();

  const run = useCallback(
    async (...args: Args): Promise<R | undefined> => {
      if (!fn) return;

      const result = fn(...args);
      if (isPromiseLike(result)) {
        setInternalPending(true);
        try {
          return await result;
        } finally {
          if (isMounted()) setInternalPending(false);
        }
      } else {
        return result;
      }
    },
    [fn, isMounted],
  );

  const isPending = Boolean(opts?.externalPending) || internalPending;

  return { run, isPending };
}
