/**
 * @vitest-environment jsdom
 *
 * The package tests in a node environment; the runtime context is read from `<meta>` tags.
 */

/**
 * Imports
 */

import { afterEach, describe, expect, it } from "vitest";
import { DeploymentStage, ExecutionEnvironment } from "../openapi-types";
import { injectMeta } from "../serve-time-injection/serveTimeInjectionTestSupport";
import { appRuntimeContext } from "./appRuntimeContext";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";

/**
 * Helpers
 */

afterEach(() => {
  document.head.replaceChildren();
});

/**
 * Tests
 */

describe("appRuntimeContext", () => {
  it("reads all three axes the deployment injected", () => {
    injectMeta(RuntimeContextMetaNames.executionEnvironment, ExecutionEnvironment.Docker);
    injectMeta(RuntimeContextMetaNames.deploymentStage, DeploymentStage.Demo);
    injectMeta(RuntimeContextMetaNames.serviceHost, "demo.elektromeister.neckar.it");

    expect(appRuntimeContext()).toEqual({
      executionEnvironment: ExecutionEnvironment.Docker,
      deploymentStage: DeploymentStage.Demo,
      serviceHost: "demo.elektromeister.neckar.it",
    });
  });

  it("reports the axes that were injected and leaves the others absent", () => {
    injectMeta(RuntimeContextMetaNames.serviceHost, "patterns.neckar.it");

    expect(appRuntimeContext()).toEqual({
      executionEnvironment: undefined,
      deploymentStage: DeploymentStage.Development,
      serviceHost: "patterns.neckar.it",
    });
  });

  //Vitest reports import.meta.env.DEV === true, so the stage takes the development-machine branch.
  it("reports the development-machine defaults when nothing was injected at all", () => {
    expect(appRuntimeContext()).toEqual({
      executionEnvironment: undefined,
      deploymentStage: DeploymentStage.Development,
      serviceHost: undefined,
    });
  });

  it("treats the unreplaced placeholders of the Vite dev server as absent on every axis", () => {
    injectMeta(RuntimeContextMetaNames.executionEnvironment, "${EXECUTION_ENVIRONMENT}");
    injectMeta(RuntimeContextMetaNames.deploymentStage, "${DEPLOYMENT_STAGE}");
    injectMeta(RuntimeContextMetaNames.serviceHost, "${SERVICE_HOST}");

    expect(appRuntimeContext()).toEqual({
      executionEnvironment: undefined,
      deploymentStage: DeploymentStage.Development,
      serviceHost: undefined,
    });
  });
});
