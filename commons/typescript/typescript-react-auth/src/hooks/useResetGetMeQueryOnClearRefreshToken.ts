/**
 * Imports
 */

import { useQueryClient } from "@neckar-hub/typescript-react-query";
import { useRefreshToken } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { useEffect, useRef } from "react";
import { getMeQueryKey } from "./useGetMeQuery";

/**
 * Hook to invalidate the cached user when the refresh token is cleared.
 * This can happen when the refresh token has expired and will force the getMe query to rerun and fail which leads to
 * showing the login screen.
 */

export function useResetGetMeQueryOnClearRefreshToken() {
  const refreshToken = useRefreshToken();
  const prevRefreshTokenRef = useRef<string | null | undefined>(undefined);
  const queryClient = useQueryClient();

  useEffect(() => {
    const prev = prevRefreshTokenRef.current;

    // Trigger only on a real "clear" transition (non-null -> null).
    if (prev !== undefined && prev !== null && refreshToken === null) {
      void (async () => {
        // Prevent overlapping refetches, then invalidate to refetch once.
        await queryClient.cancelQueries({ queryKey: getMeQueryKey });
        await queryClient.invalidateQueries({ queryKey: getMeQueryKey });
      })();
    }

    prevRefreshTokenRef.current = refreshToken;
  }, [queryClient, refreshToken]);
}
