/**
 * @vitest-environment jsdom
 *
 * The package tests in a node environment; the runtime context is read from `<meta>` tags.
 */

/**
 * Imports
 */

import { afterEach, describe, expect, it } from "vitest";
import { ExecutionEnvironment } from "../openapi-types";
import { injectMeta } from "../serve-time-injection/serveTimeInjectionTestSupport";
import { executionEnvironment, narrowExecutionEnvironment } from "./executionEnvironment";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";

/**
 * Helpers
 */

function inject(injectedExecutionEnvironment: string) {
  injectMeta(RuntimeContextMetaNames.executionEnvironment, injectedExecutionEnvironment);
}

afterEach(() => {
  document.head.replaceChildren();
});

/**
 * Tests
 */

describe("narrowExecutionEnvironment", () => {
  it("accepts every environment the generated enum knows", () => {
    for (const environment of Object.values(ExecutionEnvironment)) {
      expect(narrowExecutionEnvironment(environment)).toBe(environment);
    }
  });

  it("reports an environment it does not know as absent, rather than guessing a constant", () => {
    expect(narrowExecutionEnvironment("Nomad")).toBeUndefined();
  });

  it("reports absence as absence", () => {
    expect(narrowExecutionEnvironment(undefined)).toBeUndefined();
  });
});

describe("executionEnvironment", () => {
  it("reports the injected environment", () => {
    inject(ExecutionEnvironment.Docker);

    expect(executionEnvironment()).toBe(ExecutionEnvironment.Docker);
  });

  it("reports nothing when no meta tag was injected", () => {
    expect(executionEnvironment()).toBeUndefined();
  });

  it("reports nothing on the Vite dev server, where the placeholder stays unreplaced", () => {
    inject("${EXECUTION_ENVIRONMENT}");

    expect(executionEnvironment()).toBeUndefined();
  });

  it("reports nothing for an empty value — envsubst ran without the env var", () => {
    inject("");

    expect(executionEnvironment()).toBeUndefined();
  });
});
