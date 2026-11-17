/**
 * Global Error Listener
 *
 * A React Error Boundary only catches errors that occur during rendering.
 * All other errors as well as unhandled promise rejections can be caught by listeners on the window object.
 */

/**
 * Imports
 */

import { reactionToFailure } from "@neckar-hub/typescript-react-query";
import type { PropsWithChildren } from "react";
import { useEffect, useState } from "react";
import { type ErrorHandler, GlobalErrorFallback } from "./GlobalErrorFallback";

/**
 * Global Error Listener Component
 */

type GlobalErrorListenerProps = PropsWithChildren<{
  errorHandlers?: ErrorHandler[];
}>;

export function GlobalErrorListener(props: GlobalErrorListenerProps) {
  // Wrapped, so a rejection with a falsy reason still shows the error page.
  const [caught, setCaught] = useState<{ error: unknown } | undefined>(undefined);
  useEffect(() => {
    const handleError = (event: ErrorEvent) => {
      // workaround: dependency=pnpm:react:19.3.0 observed-on=2026-10-07 upstream=https://github.com/facebook/react/issues/10474
      // In development React rethrows errors already caught by an error boundary; those are ignored here.
      if (((error) => error.stack?.includes("invokeGuardedCallbackDev"))(new Error())) {
        return;
      }

      // Log to the devtools console in addition to setting state — error-page
      // fallbacks may be replaced/dismissed by subsequent renders, so the
      // console is the one reliable breadcrumb for post-mortem debugging.
      console.error("[GlobalErrorListener] uncaught error:", event.error);

      setCaught({ error: event.error });
    };

    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      // workaround: dependency=pnpm:react:19.3.0 observed-on=2026-10-07 upstream=https://github.com/facebook/react/issues/10474
      // In development React rethrows errors already caught by an error boundary; those are ignored here.
      if (((error) => error.stack?.includes("invokeGuardedCallbackDev"))(new Error())) {
        return;
      }

      // A toast, an alert or the auth provider answers these, for example for a form submit discarding its promise.
      const reaction = reactionToFailure(event.reason, "mutation");
      switch (reaction.type) {
        case "cancelled":
        case "notExecuted":
        case "unknownOutcome":
        case "sessionExpired":
          console.warn("[GlobalErrorListener] unhandled rejection answered elsewhere:", event.reason);
          return;
        case "permissionDenied":
        case "versionMismatch":
        case "errorPage":
          break;
        default:
          throw new Error(`Unexpected value in Switch Statement: ${JSON.stringify(reaction)}`);
      }

      console.error("[GlobalErrorListener] unhandled promise rejection:", event.reason);

      setCaught({ error: event.reason });
    };

    window.addEventListener("error", handleError);
    window.addEventListener("unhandledrejection", handleUnhandledRejection, { capture: true });

    return () => {
      window.removeEventListener("error", handleError);
      window.removeEventListener("unhandledrejection", handleUnhandledRejection, { capture: true });
    };
  }, []);

  if (caught !== undefined) {
    return <GlobalErrorFallback error={caught.error} errorCaughtBy="window-error-listener" errorHandlers={props.errorHandlers} />;
  } else {
    return <>{props.children}</>;
  }
}
