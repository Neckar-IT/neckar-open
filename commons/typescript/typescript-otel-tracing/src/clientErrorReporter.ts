import { isModuleLoadError } from "@neckar-hub/typescript-utils/type-guards";
import { isExpectedError } from "./expectedErrors";

/**
 * Kind of an error reported before the login. The collector `otlp-client-errors` drops every record with another kind.
 */
export type ClientErrorKind =
  | "ScriptError"
  | "UnhandledRejection"
  | "ChunkLoadFailed"
  | "AuthInitFailed"
  | "ConfigLoadFailed"
  | "CspViolation";

export interface ClientErrorReporterConfig {
  /** Short image digest of the running SPA, sent as attribute `version`. */
  serviceVersion: string;
}

/**
 * Path of the router `<ui-router>__client-errors` on the host of the SPA; it accepts reports without a login.
 */
const ClientErrorsPath = "/client-errors";

/**
 * Report budget of a page load; equals the burst of the router rate limit, so one page load alone stays within it.
 */
const MaxReportsPerPageLoad = 20;

/**
 * Character limits of the collector `otlp-client-errors`; truncating in the browser keeps an ASCII body below its
 * `max_request_body_size` of 16 KiB.
 */
const AttributeLengthLimits = {
  version: 64,
  message: 1024,
  stack: 8192,
  path: 512,
  userAgent: 512,
} as const;

const SeverityNumberError = 17;

interface ActiveReporter {
  serviceVersion: string;
  removeListeners: () => void;
}

let phase: "idle" | "active" | "ended" = "idle";
let activeReporter: ActiveReporter | null = null;
let sentReports = 0;

/** Every error object sent, so a second path of the same failure sends nothing. */
const reportedErrors: WeakSet<object> = new WeakSet();

/**
 * Sends every error before the login as an OTLP/JSON log record to `/client-errors`, since `/otlp/` requires the login.
 * Call once in main.tsx before `initializeOtel`; an error while its static imports evaluate stays unreported.
 */
export function installClientErrorReporter(config: ClientErrorReporterConfig): void {
  if (phase !== "idle") {
    return;
  }
  phase = "active";

  const onError = (event: ErrorEvent) => {
    reportScriptError(event.error ?? event.message);
  };
  const onUnhandledRejection = (event: PromiseRejectionEvent) => {
    const reason: unknown = event.reason;
    reportClientError(isModuleLoadError(reason) ? "ChunkLoadFailed" : "UnhandledRejection", reason);
  };
  // Vite dispatches this event with the import error as `payload` when a preloaded chunk does not load.
  const onVitePreloadError = (event: Event) => {
    reportClientError("ChunkLoadFailed", (event as Event & { payload?: unknown }).payload);
  };
  const onSecurityPolicyViolation = (event: SecurityPolicyViolationEvent) => {
    // A blocked report would otherwise report its own block.
    if (event.blockedURI.endsWith(ClientErrorsPath)) {
      return;
    }
    // A `Content-Security-Policy-Report-Only` header raises the event with disposition `report` and loads the resource.
    const outcome = event.disposition === "report" ? "reported" : "blocked";
    reportClientError("CspViolation", `${event.violatedDirective} ${outcome} ${event.blockedURI}`);
  };

  window.addEventListener("error", onError);
  window.addEventListener("unhandledrejection", onUnhandledRejection);
  window.addEventListener("vite:preloadError", onVitePreloadError);
  document.addEventListener("securitypolicyviolation", onSecurityPolicyViolation);

  activeReporter = {
    serviceVersion: config.serviceVersion,
    removeListeners: () => {
      window.removeEventListener("error", onError);
      window.removeEventListener("unhandledrejection", onUnhandledRejection);
      window.removeEventListener("vite:preloadError", onVitePreloadError);
      document.removeEventListener("securitypolicyviolation", onSecurityPolicyViolation);
    },
  };
}

/**
 * Sends [error] as a report of [kind] while the reporter is active; before `installClientErrorReporter`, after
 * `endClientErrorReporter` and for a canceled request or an HTTP 4xx the call does nothing.
 */
export function reportClientError(kind: ClientErrorKind, error: unknown): void {
  if (activeReporter === null || sentReports >= MaxReportsPerPageLoad || isExpectedError(error)) {
    return;
  }
  if (typeof error === "object" && error !== null) {
    // Vite throws a failed chunk on after `vite:preloadError`, and React.lazy hands the same object to the error boundary.
    if (reportedErrors.has(error)) {
      return;
    }
    reportedErrors.add(error);
  }
  sentReports++;
  try {
    // A failed send is swallowed: it must not raise an `unhandledrejection` that reports again.
    fetch(ClientErrorsPath, {
      method: "POST",
      keepalive: true,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(clientErrorBody(kind, error, activeReporter.serviceVersion)),
    }).catch(() => undefined);
  } catch {
    // A report never throws to its caller, whatever the browser does with the request.
  }
}

/**
 * Sends an error the SPA did not handle as `ChunkLoadFailed` when it is a failed dynamic import, else as `ScriptError`.
 */
export function reportScriptError(error: unknown): void {
  reportClientError(isModuleLoadError(error) ? "ChunkLoadFailed" : "ScriptError", error);
}

/**
 * Removes every listener of the reporter for the rest of the page load; errors go through `captureError` from now on.
 */
export function endClientErrorReporter(): void {
  activeReporter?.removeListeners();
  activeReporter = null;
  phase = "ended";
}

function clientErrorBody(kind: ClientErrorKind, error: unknown, serviceVersion: string): object {
  const { message, stack } = errorDetails(error);
  const attributes: Record<string, string> = {
    kind,
    version: truncate(serviceVersion, AttributeLengthLimits.version),
    message: truncate(message, AttributeLengthLimits.message),
    path: truncate(window.location.pathname, AttributeLengthLimits.path),
    userAgent: truncate(globalThis.navigator.userAgent, AttributeLengthLimits.userAgent),
  };
  if (stack !== "") {
    attributes["stack"] = truncate(stack, AttributeLengthLimits.stack);
  }

  return {
    resourceLogs: [
      {
        resource: { attributes: [] },
        scopeLogs: [
          {
            logRecords: [
              {
                timeUnixNano: `${Date.now()}000000`,
                severityNumber: SeverityNumberError,
                severityText: "ERROR",
                attributes: Object.entries(attributes).map(([key, value]) => ({ key, value: { stringValue: value } })),
              },
            ],
          },
        ],
      },
    ],
  };
}

function errorDetails(error: unknown): { message: string; stack: string } {
  if (error instanceof Error) {
    return { message: error.message, stack: error.stack ?? "" };
  }
  // keycloak-js rejects `init` with a plain object `{ error, error_description }` on an OAuth error; any other field of a
  // rejected object may carry user data, and the endpoint takes reports without login.
  if (typeof error === "object" && error !== null) {
    const { error: code, error_description: description } = error as { error?: unknown; error_description?: unknown };
    const oauth = [code, description].filter((part): part is string => typeof part === "string");
    return { message: oauth.length > 0 ? oauth.join(": ") : Object.prototype.toString.call(error), stack: "" };
  }
  return { message: String(error), stack: "" };
}

function truncate(value: string, maxLength: number): string {
  return value.length > maxLength ? value.slice(0, maxLength) : value;
}
