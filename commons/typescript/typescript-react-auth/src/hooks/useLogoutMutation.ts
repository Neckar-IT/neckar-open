/**
 * Imports
 */

import { useMutation, type UseMutationResult, useQueryClient } from "@neckar-hub/typescript-react-query";
import { clearTokenPair } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { useCallback } from "react";
import { useOnLogout } from "../context/LogoutContext";
import { getMeQueryKey } from "./useGetMeQuery";

/**
 * Mutation to handle the logout
 *
 * Calls the product's onLogout so the server can invalidate the refresh token chain, then clears
 * the local tokens. The `finally` signs this device out even when the server refused — but the
 * failure itself propagates, so the query client reports it and renders the error page.
 */

export function useLogoutMutation() {
  const queryClient = useQueryClient();
  const onLogout = useOnLogout();

  const rest: UseMutationResult = useMutation({
    mutationFn: async () => {
      try {
        await onLogout?.();
      } finally {
        // Runs on both paths: whatever the server answered, this device must end up signed out.
        clearTokenPair();
      }
    },
    onSuccess: async () => {
      await queryClient.resetQueries({ queryKey: getMeQueryKey });
    },
  });

  // we don't want to be forced to pass in `undefined` when calling the mutate function, so we wrap it here
  const mutate = rest.mutate;

  const logout = useCallback(() => {
    mutate(undefined);
  }, [mutate]);

  return { logout, ...rest };
}
