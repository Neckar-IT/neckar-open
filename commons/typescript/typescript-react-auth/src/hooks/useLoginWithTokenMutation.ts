/**
 * Imports
 */

import { useMutation, useQueryClient, type UseMutationResult } from "@neckar-hub/typescript-react-query";
import { setTokenPair } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import type { LoginResponse, LoginWithTokenRequest } from "@neckar-hub/typescript-utils/openapi-types";
import { getMeQueryKey } from "./useGetMeQuery";

/**
 * Error message for invalid login token
 */

export const INVALID_LOGIN_TOKEN = "invalid-login-token";

/**
 * Login With Token Mutation
 */

export function useLoginWithTokenMutation(
  onLoginWithToken: (loginWithTokenRequest: LoginWithTokenRequest) => Promise<LoginResponse>,
): UseMutationResult<LoginResponse, Error, LoginWithTokenRequest> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ token }: { token: string }) => onLoginWithToken({ token }),

    onSuccess: async (response) => {
      if (response.type === "good") {
        setTokenPair({
          accessToken: response.data.jwsTokens.accessToken,
          refreshToken: response.data.jwsTokens.refreshToken,
        });

        await queryClient.invalidateQueries({ queryKey: getMeQueryKey });
      }
    },
  });
}
