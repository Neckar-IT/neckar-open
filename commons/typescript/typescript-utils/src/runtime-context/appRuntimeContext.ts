/**
 * Imports
 */

import type { DeploymentStage, ExecutionEnvironment } from "../openapi-types";
import { deploymentStage } from "./deploymentStage";
import { executionEnvironment } from "./executionEnvironment";
import { serviceHost } from "./serviceHost";

/**
 * Resolves all three axes of the runtime context the deployment injected into the served HTML.
 *
 * Counterpart of `appImageInfo()` — same injection channel (see `serve-time-injection`), same
 * source: the compose environment of the container, set when the deployment starts it.
 * Serving it in the HTML rather than over a REST call keeps it available on the first render.
 *
 * `debugMode`, `inUnitTest` and `initialValue` of the Kotlin `RuntimeContext` are absent on purpose:
 * they are properties of the running process, not of the deployment.
 */

/**
 * The runtime context of the running frontend.
 *
 * Only the stage is non-optional, because it is the only axis with a fallback: developer-only UI
 * hangs on it, so it may never read as absent. See `deploymentStage.ts` for what that fallback is.
 */
export interface AppRuntimeContext {
  executionEnvironment: ExecutionEnvironment | undefined;
  deploymentStage: DeploymentStage;
  serviceHost: string | undefined;
}

/**
 * Reads all three axes at once. Cheap enough to call per render — three `querySelector` calls.
 *
 * Ask one of the single-axis functions, or a predicate like `isDevelopmentStage()`, when only one
 * axis matters; this exists for the cases that report or log the whole context.
 */
export function appRuntimeContext(): AppRuntimeContext {
  return {
    executionEnvironment: executionEnvironment(),
    deploymentStage: deploymentStage(),
    serviceHost: serviceHost(),
  };
}
