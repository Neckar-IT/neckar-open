/**
 * @vitest-environment jsdom
 *
 * The package tests in a node environment; the runtime context is read from `<meta>` tags.
 */

/**
 * Imports
 */

import { afterEach, describe, expect, it } from "vitest";
import { DeploymentStage } from "../openapi-types";
import { injectMeta } from "../serve-time-injection/serveTimeInjectionTestSupport";
import { deploymentStage, injectedDeploymentStage, isDemoDeployment, isDevelopmentStage, narrowDeploymentStage } from "./deploymentStage";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";

/**
 * Helpers
 */

function inject(injectedDeploymentStage: string) {
  injectMeta(RuntimeContextMetaNames.deploymentStage, injectedDeploymentStage);
}

afterEach(() => {
  document.head.replaceChildren();
});

/**
 * Tests
 */

describe("narrowDeploymentStage", () => {
  it("accepts every stage the generated enum knows", () => {
    for (const stage of Object.values(DeploymentStage)) {
      expect(narrowDeploymentStage(stage)).toBe(stage);
    }
  });

  //Vitest reports import.meta.env.DEV === true, so these runs take the development-machine branch.
  //The built-image branch (nothing injected -> Production) cannot be reached from here; it is
  //covered by the fact that a bundle from `vite build` has DEV === false.
  it("falls back to Development when nothing was injected and this is a development machine", () => {
    expect(narrowDeploymentStage(undefined)).toBe(DeploymentStage.Development);
  });

  it("falls back to Production for a stage it does not know, keeping developer-only UI locked", () => {
    expect(narrowDeploymentStage("Rollout")).toBe(DeploymentStage.Production);
  });
});

describe("deploymentStage", () => {
  it("reports the injected stage without inspecting the browser URL", () => {
    inject(DeploymentStage.Demo);

    expect(deploymentStage()).toBe(DeploymentStage.Demo);
    expect(isDemoDeployment()).toBe(true);
    expect(isDevelopmentStage()).toBe(false);
  });

  it("reports the development stage for the dev server and the local machine alike", () => {
    inject(DeploymentStage.Development);

    expect(isDevelopmentStage()).toBe(true);
    expect(isDemoDeployment()).toBe(false);
  });

  it("reports neither demo nor development on production", () => {
    inject(DeploymentStage.Production);

    expect(deploymentStage()).toBe(DeploymentStage.Production);
    expect(isDemoDeployment()).toBe(false);
    expect(isDevelopmentStage()).toBe(false);
  });

  it("falls back to Development on the Vite dev server, where the placeholder stays unreplaced", () => {
    inject("${DEPLOYMENT_STAGE}");

    expect(deploymentStage()).toBe(DeploymentStage.Development);
    expect(isDevelopmentStage()).toBe(true);
  });

  it("falls back to Development when nothing was injected at all, this being a development machine", () => {
    expect(deploymentStage()).toBe(DeploymentStage.Development);
  });

  it("falls back to Production for a stage the enum does not know", () => {
    inject("Rollout");

    expect(deploymentStage()).toBe(DeploymentStage.Production);
    expect(isDevelopmentStage()).toBe(false);
    expect(isDemoDeployment()).toBe(false);
  });
});

describe("injectedDeploymentStage", () => {
  it("reports the injected stage", () => {
    inject(DeploymentStage.Demo);

    expect(injectedDeploymentStage()).toBe(DeploymentStage.Demo);
  });

  it("reports a stage the enum does not know verbatim, where deploymentStage answers with its fallback", () => {
    inject("Rollout");

    expect(injectedDeploymentStage()).toBe("Rollout");
    expect(deploymentStage()).toBe(DeploymentStage.Production);
  });

  it("reports nothing when the tag is absent, where deploymentStage answers with its fallback", () => {
    expect(injectedDeploymentStage()).toBeUndefined();
    expect(deploymentStage()).toBe(DeploymentStage.Development);
  });

  it("reports nothing for an unreplaced placeholder", () => {
    inject("${DEPLOYMENT_STAGE}");

    expect(injectedDeploymentStage()).toBeUndefined();
  });
});
