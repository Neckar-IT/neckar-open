import { logs, SeverityNumber } from "@opentelemetry/api";

const LOGGER_NAME = "frontend-console";

type ConsoleMethod = "debug" | "log" | "info" | "warn" | "error";

/**
 * Map each patched console method to the OTel severity it should emit at.
 * `console.log` is treated as INFO — that matches Chrome/Firefox default level
 * for unlabeled logs and avoids flooding dashboards with DEBUG records from
 * production third-party libraries.
 */
const severityByMethod: Record<ConsoleMethod, { number: SeverityNumber; text: string }> = {
  debug: { number: SeverityNumber.DEBUG, text: "DEBUG" },
  log: { number: SeverityNumber.INFO, text: "INFO" },
  info: { number: SeverityNumber.INFO, text: "INFO" },
  warn: { number: SeverityNumber.WARN, text: "WARN" },
  error: { number: SeverityNumber.ERROR, text: "ERROR" },
};

let installed = false;

/**
 * Bridge every `console.{debug,log,info,warn,error}` call into an OTel log record
 * so that all frontend log output appears alongside backend logs in Loki.
 *
 * Safe to call multiple times — patches are applied only once.
 *
 * Behaviour:
 * - The native console method is still invoked, so browser DevTools output is
 *   unchanged; this only adds a parallel OTLP export.
 * - A re-entrancy guard prevents infinite loops if the OTel SDK itself logs
 *   via `console.*` while emitting.
 * - Arguments are serialised with a best-effort string conversion. Errors
 *   keep their message and stack; objects are JSON-stringified with a cyclic
 *   reference fallback to `[unserializable]`.
 *
 * Call this once in main.tsx after {@link initializeOtel}.
 */
export function installConsoleLogger(): void {
  if (installed) {
    return;
  }
  installed = true;

  (Object.keys(severityByMethod) as ConsoleMethod[]).forEach((method) => {
    const original = console[method].bind(console);
    const severity = severityByMethod[method];

    console[method] = (...args: unknown[]): void => {
      original(...args);
      emitLogRecord(severity.number, severity.text, args);
    };
  });

  console.debug("[OTEL] Console logger installed");
}

let emitting = false;

function emitLogRecord(severityNumber: SeverityNumber, severityText: string, args: unknown[]): void {
  if (emitting) {
    return;
  }
  emitting = true;
  try {
    const logger = logs.getLogger(LOGGER_NAME);
    logger.emit({
      severityNumber,
      severityText,
      body: formatArgs(args),
      attributes: {
        "browser.url": window.location.href,
      },
    });
  } catch {
    // Never let a logging failure break the host application.
  } finally {
    emitting = false;
  }
}

function formatArgs(args: unknown[]): string {
  return args.map(formatArg).join(" ");
}

function formatArg(arg: unknown): string {
  if (typeof arg === "string") return arg;
  if (arg instanceof Error) return arg.stack ?? `${arg.name}: ${arg.message}`;
  try {
    return JSON.stringify(arg);
  } catch {
    return "[unserializable]";
  }
}
