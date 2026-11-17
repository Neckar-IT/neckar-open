/**
 * Imports
 */

import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import { isExpectedApplicationError, isUnexpectedApplicationError } from "@neckar-hub/typescript-utils/type-guards";
import { isAxiosError } from "axios";
import { UnexpectedErrorPage } from "./components/UnexpectedErrorPage";

/**
 * Error Handler
 */

/**
 * `UnexpectedErrorPage` for a failed request whose body is an `ExpectedApplicationError` or `UnexpectedApplicationError` payload and
 * that no other page takes.
 */
export function applicationErrorHandler(error: unknown, errorCaughtBy: string): React.ReactNode | null {
  const reactionType = reactionToFailure(error, "query").type;
  if ((reactionType !== "errorPage" && reactionType !== "sessionExpired") || !isAxiosError(error) || error.response === undefined) {
    return null;
  }

  const payload: unknown = error.response.data;

  if (isExpectedApplicationError(payload)) {
    return (
      <UnexpectedErrorPage
        error={error}
        errorCaughtBy={errorCaughtBy}
        errorCode={payload.errorCode}
        errorMessage={payload.fallbackMessage ?? undefined}
      />
    );
  }

  if (isUnexpectedApplicationError(payload)) {
    return (
      <UnexpectedErrorPage
        error={error}
        errorCaughtBy={errorCaughtBy}
        errorMessage={payload.fallbackMessage ?? undefined}
        exceptionType={payload.exceptionType ?? undefined}
      />
    );
  }

  return null;
}
