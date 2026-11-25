/**
 * Imports
 */

import { DeploymentStage, ExecutionEnvironment } from "@neckar-hub/typescript-utils/openapi-types";
import type { DeploymentInfo } from "./deploymentInfo";

/**
 * The three states the display has to tell apart, for stories and tests alike: a deployment that
 * injected everything, one that injected nothing, and the mixed case that hides between them.
 */

export const FullyInjectedDeploymentInfo: DeploymentInfo = {
  runtimeContext: {
    executionEnvironment: ExecutionEnvironment.Docker,
    deploymentStage: DeploymentStage.Demo,
    serviceHost: "demo.app.turboangebot.de",
    effectiveDeploymentStage: DeploymentStage.Demo,
  },
  build: {
    imageDigest: "sha256:7c5587b1ca26e1af8c1a0a21a6d4bc8fc9fa726a7c5587b1ca26e1af8c1a0a21",
  },
  client: {
    browser: "Chrome",
    browserVersion: "120.0.0.0",
    operatingSystem: "Windows",
  },
};

export const NothingInjectedDeploymentInfo: DeploymentInfo = {
  runtimeContext: {
    executionEnvironment: undefined,
    deploymentStage: undefined,
    serviceHost: undefined,
    effectiveDeploymentStage: DeploymentStage.Production,
  },
  build: {
    imageDigest: undefined,
  },
  client: {
    browser: undefined,
    browserVersion: undefined,
    operatingSystem: undefined,
  },
};

export const PartiallyInjectedDeploymentInfo: DeploymentInfo = {
  runtimeContext: {
    executionEnvironment: ExecutionEnvironment.Docker,
    deploymentStage: undefined,
    serviceHost: "dev.app.turboangebot.de",
    effectiveDeploymentStage: DeploymentStage.Production,
  },
  build: {
    imageDigest: "sha256:7c5587b1ca26e1af8c1a0a21a6d4bc8fc9fa726a7c5587b1ca26e1af8c1a0a21",
  },
  client: {
    browser: "Safari",
    browserVersion: "17.1",
    operatingSystem: "macOS",
  },
};
