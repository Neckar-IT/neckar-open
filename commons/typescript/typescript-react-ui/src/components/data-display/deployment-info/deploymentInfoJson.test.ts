/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { FullyInjectedDeploymentInfo, NothingInjectedDeploymentInfo, PartiallyInjectedDeploymentInfo } from "./deploymentInfoExamples";
import { deploymentInfoAsJson } from "./deploymentInfoJson";

/**
 * Tests
 */

describe("deploymentInfoAsJson", () => {
  it("carries every axis of the running deployment, the build values and the browser it runs in", () => {
    expect(JSON.parse(deploymentInfoAsJson(FullyInjectedDeploymentInfo))).toEqual({
      executionEnvironment: "Docker",
      deploymentStage: "Demo",
      effectiveDeploymentStage: "Demo",
      serviceHost: "demo.app.turboangebot.de",
      imageDigest: "sha256:7c5587b1ca26e1af8c1a0a21a6d4bc8fc9fa726a7c5587b1ca26e1af8c1a0a21",
      imageDigestShort: "7c5587b1ca26",
      browser: "Chrome",
      browserVersion: "120.0.0.0",
      operatingSystem: "Windows",
    });
  });

  it("keeps an absent value as an explicit null instead of dropping its key, the derived short digest included", () => {
    expect(JSON.parse(deploymentInfoAsJson(NothingInjectedDeploymentInfo))).toEqual({
      executionEnvironment: null,
      deploymentStage: null,
      effectiveDeploymentStage: "Production",
      serviceHost: null,
      imageDigest: null,
      imageDigestShort: null,
      browser: null,
      browserVersion: null,
      operatingSystem: null,
    });
  });

  it("keeps reported and absent values apart in the same object", () => {
    expect(JSON.parse(deploymentInfoAsJson(PartiallyInjectedDeploymentInfo))).toEqual({
      executionEnvironment: "Docker",
      deploymentStage: null,
      effectiveDeploymentStage: "Production",
      serviceHost: "dev.app.turboangebot.de",
      imageDigest: "sha256:7c5587b1ca26e1af8c1a0a21a6d4bc8fc9fa726a7c5587b1ca26e1af8c1a0a21",
      imageDigestShort: "7c5587b1ca26",
      browser: "Safari",
      browserVersion: "17.1",
      operatingSystem: "macOS",
    });
  });

  it("formats indented, so the pasted block stays readable in a support request", () => {
    expect(deploymentInfoAsJson(PartiallyInjectedDeploymentInfo)).toBe(
      [
        "{",
        `  "executionEnvironment": "Docker",`,
        `  "deploymentStage": null,`,
        `  "effectiveDeploymentStage": "Production",`,
        `  "serviceHost": "dev.app.turboangebot.de",`,
        `  "imageDigest": "sha256:7c5587b1ca26e1af8c1a0a21a6d4bc8fc9fa726a7c5587b1ca26e1af8c1a0a21",`,
        `  "imageDigestShort": "7c5587b1ca26",`,
        `  "browser": "Safari",`,
        `  "browserVersion": "17.1",`,
        `  "operatingSystem": "macOS"`,
        "}",
      ].join("\n"),
    );
  });
});
