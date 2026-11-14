/**
 * Imports
 */

import { shortImageDigest } from "@neckar-hub/typescript-utils/deploy-metadata";
import type { DeploymentInfo } from "./deploymentInfo";

/**
 * Everything the overlay knows as JSON, for pasting into a support request — the values the dialog
 * displays and the ones it leaves out.
 *
 * `null` carries what the display says in words: nothing was injected. A dropped key would read as a
 * value nobody thought to send, which is the misunderstanding the whole overlay exists to prevent.
 */
export function deploymentInfoAsJson({ runtimeContext, build, client }: DeploymentInfo): string {
  return asJson({
    executionEnvironment: runtimeContext.executionEnvironment,
    deploymentStage: runtimeContext.deploymentStage,
    effectiveDeploymentStage: runtimeContext.effectiveDeploymentStage,
    serviceHost: runtimeContext.serviceHost,
    imageDigest: build.imageDigest,
    imageDigestShort: build.imageDigest === undefined ? undefined : shortImageDigest(build.imageDigest),
    browser: client.browser,
    browserVersion: client.browserVersion,
    operatingSystem: client.operatingSystem,
  });
}

/**
 * `JSON.stringify` drops an `undefined` property; the explicit `null` keeps every field in the
 * output.
 */
function asJson(values: Record<string, string | undefined>): string {
  return JSON.stringify(Object.fromEntries(Object.entries(values).map(([key, value]) => [key, value ?? null])), null, 2);
}
