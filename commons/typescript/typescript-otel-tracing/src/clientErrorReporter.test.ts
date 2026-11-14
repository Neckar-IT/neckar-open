import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type * as ClientErrorReporter from "./clientErrorReporter";

interface SentRecord {
  url: string;
  init: RequestInit;
  attributes: Record<string, string>;
}

const fetchSpy = vi.fn<typeof fetch>();
let reporter: typeof ClientErrorReporter;

function sentRecords(): SentRecord[] {
  return fetchSpy.mock.calls.map(([url, init]) => {
    const payload = JSON.parse(init?.body as string) as {
      resourceLogs: {
        scopeLogs: {
          logRecords: { attributes: { key: string; value: { stringValue: string } }[] }[];
        }[];
      }[];
    };
    const record = payload.resourceLogs[0]?.scopeLogs[0]?.logRecords[0];
    if (record === undefined) {
      throw new Error("Expected one log record in the body");
    }
    return {
      url: url as string,
      init: init ?? {},
      attributes: Object.fromEntries(record.attributes.map((attribute) => [attribute.key, attribute.value.stringValue])),
    };
  });
}

function onlyRecord(): SentRecord {
  const records = sentRecords();
  expect(records).toHaveLength(1);
  return records[0] as SentRecord;
}

function dispatchUnhandledRejection(reason: unknown): void {
  // jsdom implements no PromiseRejectionEvent.
  window.dispatchEvent(Object.assign(new Event("unhandledrejection"), { reason }));
}

function dispatchSecurityPolicyViolation(
  violatedDirective: string,
  blockedURI: string,
  disposition: SecurityPolicyViolationEventDisposition = "enforce",
): void {
  // jsdom implements no SecurityPolicyViolationEvent.
  document.dispatchEvent(Object.assign(new Event("securitypolicyviolation"), { violatedDirective, blockedURI, disposition }));
}

describe("installClientErrorReporter", () => {
  beforeEach(async () => {
    vi.resetModules();
    fetchSpy.mockReset();
    fetchSpy.mockResolvedValue(new Response(null, { status: 200 }));
    vi.stubGlobal("fetch", fetchSpy);
    window.history.replaceState(null, "", "/ideas/42?filter=open#top");
    reporter = await import("./clientErrorReporter");
    reporter.installClientErrorReporter({ serviceVersion: "3f2a9c1d" });
  });

  afterEach(() => {
    reporter.endClientErrorReporter();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("sends a window error as ScriptError in an OTLP/JSON body to /client-errors", () => {
    const error = new Error("cannot read id");
    window.dispatchEvent(new ErrorEvent("error", { error, message: error.message }));

    const record = onlyRecord();
    expect(record.url).toBe("/client-errors");
    expect(record.init.method).toBe("POST");
    expect(record.init.keepalive).toBe(true);
    expect(record.attributes).toEqual({
      kind: "ScriptError",
      version: "3f2a9c1d",
      message: "cannot read id",
      stack: error.stack,
      path: "/ideas/42",
      userAgent: globalThis.navigator.userAgent,
    });
  });

  it("sends an unhandled rejection as UnhandledRejection", () => {
    dispatchUnhandledRejection(new Error("request aborted"));

    expect(onlyRecord().attributes["kind"]).toBe("UnhandledRejection");
  });

  it("sends nothing for a rejected HTTP 4xx request", () => {
    dispatchUnhandledRejection(
      Object.assign(new Error("Request failed with status code 401"), { isAxiosError: true, response: { status: 401 } }),
    );

    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("sends nothing for a canceled request", () => {
    dispatchUnhandledRejection(Object.assign(new Error("canceled"), { __CANCEL__: true }));

    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("sends a failed dynamic import as ChunkLoadFailed", () => {
    const importError = new TypeError("error loading dynamically imported module: https://ideas.neckar.it/assets/IdeaPage-a1b2c3.js");
    window.dispatchEvent(new ErrorEvent("error", { error: importError, message: importError.message }));
    dispatchUnhandledRejection(
      new TypeError("Failed to fetch dynamically imported module: https://ideas.neckar.it/assets/IdeaPage-a1b2c3.js"),
    );
    window.dispatchEvent(
      Object.assign(new Event("vite:preloadError"), { payload: new Error("Unable to preload CSS for /assets/IdeaPage-a1b2c3.css") }),
    );

    expect(sentRecords().map((record) => record.attributes["kind"])).toEqual(["ChunkLoadFailed", "ChunkLoadFailed", "ChunkLoadFailed"]);
  });

  it("sends a CSP violation as CspViolation", () => {
    dispatchSecurityPolicyViolation("script-src-elem", "https://cdn.example.com/widget.js");

    const record = onlyRecord();
    expect(record.attributes["kind"]).toBe("CspViolation");
    expect(record.attributes["message"]).toBe("script-src-elem blocked https://cdn.example.com/widget.js");
    expect(record.attributes["stack"]).toBeUndefined();
  });

  it("names a CSP violation of a report-only policy as reported", () => {
    dispatchSecurityPolicyViolation("script-src-elem", "https://cdn.example.com/widget.js", "report");

    expect(onlyRecord().attributes["message"]).toBe("script-src-elem reported https://cdn.example.com/widget.js");
  });

  it("sends nothing for a CSP violation of its own report", () => {
    dispatchSecurityPolicyViolation("connect-src", "https://ideas.neckar.it/client-errors");

    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it.each([
    ["AuthInitFailed", "keycloak.init timed out after 20000 ms"],
    ["ConfigLoadFailed", "config.json answered 503"],
  ] as const)("sends a reported %s", (kind, message) => {
    reporter.reportClientError(kind, new Error(message));

    const record = onlyRecord();
    expect(record.attributes["kind"]).toBe(kind);
    expect(record.attributes["message"]).toBe(message);
  });

  it("sends the OAuth error object of keycloak-js as code and description", () => {
    reporter.reportClientError("AuthInitFailed", { error: "access_denied", error_description: "consent rejected" });

    expect(onlyRecord().attributes["message"]).toBe("access_denied: consent rejected");
  });

  it("sends no field of another rejected object", () => {
    dispatchUnhandledRejection({ email: "jschneider@neckar.it", accessToken: "eyJ0eXAi" });

    expect(onlyRecord().attributes["message"]).toBe("[object Object]");
  });

  it("sends a chunk error once when Vite throws it on after vite:preloadError", () => {
    const chunkError = new TypeError("Failed to fetch dynamically imported module: https://ideas.neckar.it/assets/IdeaPage-a1b2c3.js");
    window.dispatchEvent(Object.assign(new Event("vite:preloadError"), { payload: chunkError }));
    reporter.reportScriptError(chunkError);

    expect(sentRecords()).toHaveLength(1);
  });

  it("truncates every value to the collector limits", async () => {
    reporter.endClientErrorReporter();
    vi.resetModules();
    window.history.replaceState(null, "", `/${"p".repeat(600)}`);
    vi.spyOn(globalThis.navigator, "userAgent", "get").mockReturnValue("u".repeat(600));
    reporter = await import("./clientErrorReporter");
    reporter.installClientErrorReporter({ serviceVersion: "v".repeat(100) });
    const error = new Error("m".repeat(2000));
    error.stack = "s".repeat(10_000);

    reporter.reportClientError("ScriptError", error);

    const { attributes } = onlyRecord();
    expect(attributes["message"]).toHaveLength(1024);
    expect(attributes["stack"]).toHaveLength(8192);
    expect(attributes["version"]).toHaveLength(64);
    expect(attributes["path"]).toHaveLength(512);
    expect(attributes["userAgent"]).toHaveLength(512);
  });

  it("sends no further report when the send fails", async () => {
    fetchSpy.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    reporter.reportClientError("AuthInitFailed", new Error("keycloak.init failed"));
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(fetchSpy).toHaveBeenCalledOnce();
  });

  it("sends a cyclic error object without serializing it", () => {
    const cyclic: Record<string, unknown> = {};
    cyclic["self"] = cyclic;

    expect(() => reporter.reportClientError("UnhandledRejection", cyclic)).not.toThrow();
    expect(onlyRecord().attributes["message"]).toBe("[object Object]");
  });

  it("adds its listeners once on a second install", () => {
    reporter.installClientErrorReporter({ serviceVersion: "3f2a9c1d" });

    dispatchUnhandledRejection(new Error("request aborted"));

    expect(fetchSpy).toHaveBeenCalledOnce();
  });

  it("sends nothing after an install that follows endClientErrorReporter", () => {
    reporter.endClientErrorReporter();
    reporter.installClientErrorReporter({ serviceVersion: "3f2a9c1d" });

    reporter.reportClientError("AuthInitFailed", new Error("after login"));

    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("stops after 20 reports per page load", () => {
    for (let index = 0; index < 21; index++) {
      reporter.reportClientError("ScriptError", new Error(`error ${index}`));
    }

    expect(fetchSpy).toHaveBeenCalledTimes(20);
  });

  it("sends nothing after endClientErrorReporter", () => {
    reporter.endClientErrorReporter();

    // Without its listener the event reaches the uncaught-error handler of Vitest, which reads `error`.
    window.dispatchEvent(new ErrorEvent("error", { message: "after login" }));
    dispatchUnhandledRejection(new Error("after login"));
    dispatchSecurityPolicyViolation("script-src-elem", "https://cdn.example.com/widget.js");
    reporter.reportClientError("AuthInitFailed", new Error("after login"));

    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
