/**
 * Imports
 */

import { isTestMode } from "@neckar-hub/typescript-react-utils/is-test-mode";
import { readClientEnvironment, type ClientEnvironment } from "@neckar-hub/typescript-utils/client-environment";
import { imageDigest } from "@neckar-hub/typescript-utils/deploy-metadata";
import type { DeploymentStage, ExecutionEnvironment } from "@neckar-hub/typescript-utils/openapi-types";
import { deploymentStage, executionEnvironment, injectedDeploymentStage, serviceHost } from "@neckar-hub/typescript-utils/runtime-context";

/**
 * What a support request needs about the running frontend, with absence kept as absence.
 *
 * Three categories that must not be flattened into one another: the runtime context differs per
 * deployment, the build value names the image and is the same wherever it runs, the client values
 * belong to the browser in front of it and to no deployment at all.
 */
export interface DeploymentInfo {
  runtimeContext: InjectedRuntimeContext;
  build: InjectedBuildInfo;
  client: ClientEnvironment;
}

/**
 * The runtime context as the deployment injected it — `undefined` per axis where nothing arrived.
 */
export interface InjectedRuntimeContext {
  executionEnvironment: ExecutionEnvironment | undefined;
  deploymentStage: string | undefined;
  serviceHost: string | undefined;
  /**
   * The stage the application actually works with. Differs from [deploymentStage] when nothing was
   * injected or when an unknown stage was, both of which resolve to the fail-closed `Production`.
   */
  effectiveDeploymentStage: DeploymentStage;
}

/**
 * The digest of the image the frontend is served from — `undefined` where nothing arrived.
 *
 * `imageDigestShort` is absent because it is derived, not injected.
 */
export interface InjectedBuildInfo {
  imageDigest: string | undefined;
}

/**
 * Build value shown under screenshot tests: the running digest would change every screenshot with
 * every content change.
 */
const TestModeBuildInfo: InjectedBuildInfo = {
  imageDigest: "sha256:ae4608c7c59006b1cd2f4a9e83b5d07f1a6c2e94ae4608c7c59006b1cd2f4a9e",
};

/**
 * The build value to display: the injected one, or a fixed one under screenshot tests.
 */
export function displayedBuildInfo(injected: InjectedBuildInfo): InjectedBuildInfo {
  return isTestMode() ? TestModeBuildInfo : injected;
}

/**
 * Reads what the deployment and the image injected into the served HTML, plus what the browser
 * says about itself.
 *
 * Goes through the single-axis resolvers rather than `appRuntimeContext()` and `appImageInfo()`:
 * those two answer an absent value with a fallback, and telling the fallback from a reported value
 * is what this overlay exists for.
 */
export function readDeploymentInfo(): DeploymentInfo {
  return {
    runtimeContext: {
      executionEnvironment: executionEnvironment(),
      deploymentStage: injectedDeploymentStage(),
      serviceHost: serviceHost(),
      effectiveDeploymentStage: deploymentStage(),
    },
    build: displayedBuildInfo({ imageDigest: imageDigest() }),
    client: readClientEnvironment(),
  };
}
