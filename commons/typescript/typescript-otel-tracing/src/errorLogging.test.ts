import { type Logger, type LogRecord, logs } from "@opentelemetry/api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { captureError } from "./errorLogging";

const { reportScriptError } = vi.hoisted(() => ({ reportScriptError: vi.fn() }));

vi.mock("./clientErrorReporter", () => ({ reportScriptError }));

interface CapturedRecord {
  body: string;
  severityText: string;
  attributes: Record<string, string | number>;
}

function getEmittedRecord(spy: ReturnType<typeof vi.fn>): CapturedRecord {
  const firstCall = spy.mock.calls[0];
  if (firstCall === undefined) {
    throw new Error("Expected the logger emit spy to have been called at least once");
  }
  return firstCall[0] as CapturedRecord;
}

describe("captureError", () => {
  const emitSpy = vi.fn<(record: LogRecord) => void>();

  beforeEach(() => {
    emitSpy.mockClear();
    reportScriptError.mockClear();
    vi.spyOn(logs, "getLogger").mockReturnValue({ emit: emitSpy } as unknown as Logger);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("emits a log record for an Error", () => {
    const error = new TypeError("something broke");

    captureError(error, "window-error");

    expect(emitSpy).toHaveBeenCalledOnce();
    const record = getEmittedRecord(emitSpy);
    expect(record.body).toBe("something broke");
    expect(record.severityText).toBe("ERROR");
    expect(record.attributes["error.type"]).toBe("TypeError");
    expect(record.attributes["error.message"]).toBe("something broke");
    expect(record.attributes["error.source"]).toBe("window-error");
    expect(record.attributes["error.stack"]).toBeDefined();
  });

  it("emits a log record for a non-Error value", () => {
    captureError("string error", "manual");

    expect(emitSpy).toHaveBeenCalledOnce();
    const record = getEmittedRecord(emitSpy);
    expect(record.body).toBe("string error");
    expect(record.attributes["error.type"]).toBe("string");
    expect(record.attributes["error.stack"]).toBe("");
  });

  it.each(["error-boundary", "mutation", "manual"] as const)("hands a %s error to the error reporter before the login", (source) => {
    const error = new Error("render failed");

    captureError(error, source);

    expect(reportScriptError).toHaveBeenCalledExactlyOnceWith(error);
  });

  it.each(["window-error", "unhandled-rejection"] as const)("leaves a %s to the listeners of the error reporter", (source) => {
    captureError(new Error("uncaught"), source);

    expect(reportScriptError).not.toHaveBeenCalled();
  });

  it("skips 4xx Axios errors (handled client-side, no log noise)", () => {
    const axiosError = new Error("Request failed with status code 404") as Error & {
      isAxiosError: boolean;
      response: { status: number };
    };
    axiosError.isAxiosError = true;
    axiosError.response = { status: 404 };

    captureError(axiosError, "error-boundary");

    expect(emitSpy).not.toHaveBeenCalled();
    expect(reportScriptError).not.toHaveBeenCalled();
  });

  it("emits 5xx Axios errors with status code and component stack", () => {
    const axiosError = new Error("Request failed with status code 500") as Error & {
      isAxiosError: boolean;
      response: { status: number };
    };
    axiosError.isAxiosError = true;
    axiosError.response = { status: 500 };

    captureError(axiosError, "error-boundary");

    expect(emitSpy).toHaveBeenCalledOnce();
    const record = getEmittedRecord(emitSpy);
    expect(record.body).toBe("Request failed with status code 500");
    expect(record.attributes["http.response.status_code"]).toBe(500);
    expect(record.attributes["error.source"]).toBe("error-boundary");
    expect(record.attributes["error.stack"]).toBeDefined();
  });

  it("skips canceled requests (AbortController)", () => {
    const canceledError = new Error("canceled") as Error & {
      __CANCEL__: boolean;
    };
    canceledError.__CANCEL__ = true;

    captureError(canceledError, "unhandled-rejection");

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it("captures Axios errors without a response (network errors)", () => {
    const axiosError = new Error("Network Error") as Error & {
      isAxiosError: boolean;
      response: undefined;
    };
    axiosError.isAxiosError = true;
    axiosError.response = undefined;

    captureError(axiosError, "window-error");

    expect(emitSpy).toHaveBeenCalledOnce();
  });
});
