/**
 * Imports
 */

import { isRouteErrorResponse } from "react-router";
import { formatErrorBody, MaxThrownValueLength } from "../formatErrorBody";
import { UnexpectedErrorPage } from "./components/UnexpectedErrorPage";

/**
 * Unknown Error Handler
 *
 * Closes the handler chain and takes every value that reaches it, so a rejected string still leaves the user an error page.
 */

export function unknownErrorHandler(error: unknown, errorCaughtBy: string): React.ReactElement {
  const { errorMessage, stackTrace } = describeThrownValue(error);
  return <UnexpectedErrorPage error={error} errorCaughtBy={errorCaughtBy} errorMessage={errorMessage} stackTrace={stackTrace} />;
}

/**
 * Message and stack trace of a thrown value that need not be an `Error`.
 */
export function describeThrownValue(error: unknown): { errorMessage: string; stackTrace?: string } {
  if (error instanceof Error) {
    return { errorMessage: error.message, stackTrace: error.stack };
  }

  // An unknown route yields an ErrorResponse with an Error cause; a thrown Response or data() carries its detail in `data`.
  if (isRouteErrorResponse(error)) {
    const cause: unknown = "error" in error ? error.error : undefined;
    if (cause instanceof Error) {
      return { errorMessage: cause.message, stackTrace: cause.stack };
    }
    const status = `${error.status} ${error.statusText}`.trim();
    const detail = formatErrorBody(error.data, MaxThrownValueLength);
    return { errorMessage: detail === undefined ? status : `${status}: ${detail}` };
  }

  switch (typeof error) {
    case "undefined":
      return { errorMessage: "undefined" };
    case "boolean":
    case "number":
    case "symbol":
    case "bigint":
      return { errorMessage: String(error) };
    case "string":
    case "object":
    case "function":
      // formatErrorBody declines null and "", which JSON.stringify spells out.
      return { errorMessage: formatErrorBody(error, MaxThrownValueLength) ?? JSON.stringify(error) };
    default:
      throw new Error("typeof returned a type this switch does not know");
  }
}
