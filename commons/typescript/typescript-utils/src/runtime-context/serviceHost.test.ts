/**
 * @vitest-environment jsdom
 *
 * The package tests in a node environment; the runtime context is read from `<meta>` tags.
 */

/**
 * Imports
 */

import { afterEach, describe, expect, it } from "vitest";
import { injectMeta } from "../serve-time-injection/serveTimeInjectionTestSupport";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";
import { serviceHost } from "./serviceHost";

/**
 * Helpers
 */

function inject(injectedServiceHost: string) {
  injectMeta(RuntimeContextMetaNames.serviceHost, injectedServiceHost);
}

afterEach(() => {
  document.head.replaceChildren();
});

/**
 * Tests
 */

describe("serviceHost", () => {
  it("reports the injected hostname", () => {
    inject("dev.elektromeister.neckar.it");

    expect(serviceHost()).toBe("dev.elektromeister.neckar.it");
  });

  it("reports nothing when no meta tag was injected", () => {
    expect(serviceHost()).toBeUndefined();
  });

  it("reports nothing on the Vite dev server, where the placeholder stays unreplaced", () => {
    inject("${SERVICE_HOST}");

    expect(serviceHost()).toBeUndefined();
  });

  it("reports nothing for an empty value — envsubst ran without the env var", () => {
    inject("");

    expect(serviceHost()).toBeUndefined();
  });
});
