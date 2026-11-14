/**
 * Imports
 */

import { ExecutionEnvironment } from "../openapi-types";
import { readInjectedMeta } from "../serve-time-injection";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";

/**
 * What kind of environment runs this frontend — `undefined` when the deployment did not say.
 *
 * The enum comes from the shared spec (`RestModelOpenApiGeneratorTest` registers
 * `RuntimeContextDetails`), so frontend and Kotlin side know the same set of environments.
 */
export function executionEnvironment(): ExecutionEnvironment | undefined {
  return narrowExecutionEnvironment(readInjectedMeta(RuntimeContextMetaNames.executionEnvironment));
}

/**
 * Narrows an injected string to the generated enum, and reports absence as absence.
 *
 * Unlike {@link narrowDeploymentStage} this has no fallback constant: no UI hangs on this axis, so
 * no value here is the restrictive one, and every constant as a fallback would be a claim about an
 * environment the page knows nothing about. The call site sees the `undefined` and decides.
 *
 * Derived from `Object.values` rather than a `switch`, so a new
 * `it.neckar.runtime.context.ExecutionEnvironment` constant needs no change here.
 */
export function narrowExecutionEnvironment(raw: string | undefined): ExecutionEnvironment | undefined {
  return knownExecutionEnvironments.find((environment) => environment === raw);
}

/**
 * Every environment the generated enum knows.
 */
const knownExecutionEnvironments: readonly ExecutionEnvironment[] = Object.values(ExecutionEnvironment);
