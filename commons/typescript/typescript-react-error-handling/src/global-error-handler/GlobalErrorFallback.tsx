/**
 * Imports
 */

import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { AxiosError, isAxiosError } from "axios";
import { Fragment, useEffect, useState } from "react";
import { applicationErrorHandler } from "./default-error-handlers/applicationErrorHandler";
import { missingCapabilityErrorHandler } from "./default-error-handlers/missingCapabilityErrorHandler";
import { serviceUnavailableErrorHandler } from "./default-error-handlers/serviceUnavailableErrorHandler";
import { unknownErrorHandler } from "./default-error-handlers/unknownErrorHandler";
import { versionMismatchErrorHandler } from "./default-error-handlers/versionMismatchErrorHandler";

/**
 * Type Declarations
 */

export type ErrorHandler = (error: unknown, errorCaughtBy: string) => React.ReactNode | null;

interface GlobalErrorFallbackProps {
  error: unknown;
  errorCaughtBy: string;
  errorHandlers?: ErrorHandler[];
  /** Fetches again once the service is available; `ServiceUnavailablePage` offers no recovery without it. */
  recover?: () => void;
}

export function GlobalErrorFallback(props: GlobalErrorFallbackProps): React.ReactNode {
  const [errorPage, setErrorPage] = useState<{ component: React.ReactNode; sequence: number } | null>(null);

  useEffect(() => {
    // Handlers may await, so an older error must not replace the page of a newer one when it resolves late.
    let superseded = false;

    // The default handlers take disjoint reactions of reactionToFailure; unknownErrorHandler closes the chain.
    const errorHandlers = [
      ...(props.errorHandlers ?? []),
      versionMismatchErrorHandler,
      (error: unknown) => serviceUnavailableErrorHandler(error, props.recover),
      missingCapabilityErrorHandler,
      applicationErrorHandler,
      unknownErrorHandler,
    ];

    const handleError = async () => {
      const error = await withJsonBlobPayload(props.error);
      if (superseded) {
        return;
      }

      for (const handler of errorHandlers) {
        // eslint-disable-next-line no-await-in-loop -- handlers run in priority order and the loop breaks at the first match; parallelising would lose the ordering and run handlers needlessly.
        const result = await handler(error, props.errorCaughtBy);
        if (superseded) {
          return;
        }

        if (isDefined(result)) {
          setErrorPage((previous) => ({ component: result, sequence: (previous?.sequence ?? 0) + 1 }));
          return;
        }
      }

      throw new Error("No error handler matched, although unknownErrorHandler takes every value");
    };

    void handleError();

    return () => {
      superseded = true;
    };
  }, [props.errorHandlers, props.error, props.errorCaughtBy, props.recover]);

  if (errorPage === null) {
    return null;
  }

  // A new key per resolved error remounts the page, so page state such as the time in [ErrorSupportActions] belongs to that error.
  return <Fragment key={errorPage.sequence}>{errorPage.component}</Fragment>;
}

/**
 * The error with a JSON body read out of a Blob, so every handler sees the payload of a request that expected a file.
 */
async function withJsonBlobPayload(error: unknown): Promise<unknown> {
  if (!isAxiosError(error) || !(error.response?.data instanceof Blob)) {
    return error;
  }

  try {
    const payload = JSON.parse(await error.response.data.text()) as unknown;
    return new AxiosError(error.message, error.code, error.config, error.request, { ...error.response, data: payload });
  } catch {
    // A Blob that is no JSON carries no payload.
    return error;
  }
}
