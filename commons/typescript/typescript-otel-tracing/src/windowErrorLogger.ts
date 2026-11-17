import { captureError } from "./errorLogging";

let installed = false;

/**
 * Install global error event listeners that forward uncaught errors
 * and unhandled promise rejections to the OTEL logging pipeline.
 *
 * Safe to call multiple times — listeners are only installed once.
 * Call this once in main.tsx after initializeOtel().
 */
export function installWindowErrorLogger(): void {
  if (installed) {
    return;
  }
  installed = true;

  window.addEventListener("error", (event: ErrorEvent) => {
    captureError(event.error ?? event.message, "window-error");
  });

  window.addEventListener("unhandledrejection", (event: PromiseRejectionEvent) => {
    captureError(event.reason, "unhandled-rejection");
  });

  console.debug("[OTEL] Window error logger installed");
}
