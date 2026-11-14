/**
 * Imports
 */

import { appImageInfo } from "@neckar-hub/typescript-utils/deploy-metadata";
import type { PropsWithChildren } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { type ErrorHandler, GlobalErrorFallback } from "./GlobalErrorFallback";
import { GlobalErrorListener } from "./GlobalErrorListener";
import { type ObservabilityConfig, ObservabilityProvider } from "./ObservabilityContext";
import { SupportContactProvider } from "./SupportContactContext";
import { type VersionInfoData, VersionInfoProvider } from "./VersionInfoContext";

/**
 * Global Error Handler Component
 */

/**
 * Callback invoked when an error is caught, before the fallback UI is rendered.
 * Can be used to send errors to external logging systems (e.g., OTEL logs).
 */
export type ErrorCallback = (error: unknown, caughtBy: string) => void;

type GlobalErrorHandlerProps = PropsWithChildren<{
  errorHandlers?: ErrorHandler[];
  /**
   * Version of the running frontend. Defaults to the git metadata injected into the served HTML
   * (#2625) — pass it explicitly only to show a fixed version, e.g. in tests and stories.
   */
  versionInfo?: VersionInfoData;
  observability?: ObservabilityConfig;
  /** Recipient of the support mail on the error pages; [DefaultSupportContactEmail] when absent. */
  contactEmail?: string;
  onError?: ErrorCallback;
}>;

export function GlobalErrorHandler(props: GlobalErrorHandlerProps) {
  return (
    <VersionInfoProvider versionInfo={props.versionInfo ?? appImageInfo()}>
      <ObservabilityProvider observability={props.observability}>
        <SupportContactProvider contactEmail={props.contactEmail}>
          <GlobalErrorListener errorHandlers={props.errorHandlers}>
            <ErrorBoundary
              onError={(error, info) => {
                // Log first so the stack trace lands in the devtools console even
                // if the fallback renders briefly or is dismissed by a subsequent
                // auto-reload.
                console.error("[GlobalErrorHandler] error caught by boundary:", error, info);
                props.onError?.(error, "error-boundary");
              }}
              fallbackRender={({ error }) => (
                <GlobalErrorFallback error={error as unknown} errorCaughtBy="global-error-boundary" errorHandlers={props.errorHandlers} />
              )}
            >
              {props.children}
            </ErrorBoundary>
          </GlobalErrorListener>
        </SupportContactProvider>
      </ObservabilityProvider>
    </VersionInfoProvider>
  );
}
