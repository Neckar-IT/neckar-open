/**
 * @vitest-environment jsdom
 *
 * The package tests in a node environment; `readInjectedMeta` needs a DOM.
 */

/**
 * Imports
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import { readInjectedMeta, sanitizeInjectedValue } from "./serveTimeInjection";
import { injectMeta } from "./serveTimeInjectionTestSupport";

/**
 * Helpers
 */

afterEach(() => {
  //Before touching document: one test stubs it away, and the stub outlives the test body.
  vi.unstubAllGlobals();
  document.head.replaceChildren();
});

/**
 * Tests
 */

describe("sanitizeInjectedValue", () => {
  it("passes a substituted value through", () => {
    expect(sanitizeInjectedValue("Demo")).toBe("Demo");
  });

  it("treats an empty value as absent — envsubst ran without the env var", () => {
    expect(sanitizeInjectedValue("")).toBeUndefined();
  });

  it("treats an unreplaced placeholder as absent — HTML served without the injecting entrypoint", () => {
    expect(sanitizeInjectedValue("${DEPLOYMENT_STAGE}")).toBeUndefined();
  });
});

describe("readInjectedMeta", () => {
  it("reads the content of the meta tag", () => {
    injectMeta("runtime-context.deploymentStage", "Demo");

    expect(readInjectedMeta("runtime-context.deploymentStage")).toBe("Demo");
  });

  it("reports nothing for a meta tag the head does not carry", () => {
    expect(readInjectedMeta("runtime-context.deploymentStage")).toBeUndefined();
  });

  it("reports nothing for a meta tag without a content attribute", () => {
    document.head.appendChild(Object.assign(document.createElement("meta"), { name: "runtime-context.serviceHost" }));

    expect(readInjectedMeta("runtime-context.serviceHost")).toBeUndefined();
  });

  it("applies the sanitizing rule to the content it read", () => {
    injectMeta("empty", "");
    injectMeta("placeholder", "${SERVICE_HOST}");

    expect(readInjectedMeta("empty")).toBeUndefined();
    expect(readInjectedMeta("placeholder")).toBeUndefined();
  });

  it("reports nothing outside a DOM — a plain vitest run has no document", () => {
    vi.stubGlobal("document", undefined);

    expect(readInjectedMeta("runtime-context.deploymentStage")).toBeUndefined();
  });
});
