/**
 * Imports
 */

import { useMutation, useQueryClient, type UseMutationResult } from "@neckar-hub/typescript-react-query";
import { setTokenPair } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { type LoginRequest, type LoginResponse } from "@neckar-hub/typescript-utils/openapi-types";
import { getMeQueryKey } from "./useGetMeQuery";

/**
 * Login Mutation
 *
 * This will send a login request to the server, set the token pair in local storage and try to refetch the user.
 */

export function useLoginMutation(
  postLogin: (loginRequest: LoginRequest) => Promise<LoginResponse>,
): UseMutationResult<LoginResponse, Error, LoginRequest> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (credentials: LoginRequest) => postLogin(credentials),
    meta: {
      // Don't invalidate all queries (especially getMe) after login: it would rerender the login screen and lose the
      // error message for invalid credentials.
      skipGlobalQueryResets: true,
    },
    onSuccess: async (response) => {
      if (response.type === "good") {
        setTokenPair({
          accessToken: response.data.jwsTokens.accessToken,
          refreshToken: response.data.jwsTokens.refreshToken,
        });

        // Automatic invalidation is skipped (see above), so invalidate getMe explicitly to refetch the user.
        await queryClient.invalidateQueries({ queryKey: getMeQueryKey });
      }
    },
  });
}
