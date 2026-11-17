/**
 * Imports
 */

import { DeploymentStage } from "../openapi-types";
import { readInjectedMeta } from "../serve-time-injection";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";

/**
 * The deployment stage this frontend is served on.
 *
 * Resolved from the meta tag the nginx entrypoint substitutes into the served HTML, so it is
 * available synchronously on the first render — no request, no stage-specific UI appearing late.
 *
 * The enum comes from the shared spec (`RestModelOpenApiGeneratorTest` registers
 * `RuntimeContextDetails`), so every frontend narrows against one generated set of stages rather
 * than the copy its own meta endpoint happens to produce.
 */
export function deploymentStage(): DeploymentStage {
  return narrowDeploymentStage(injectedDeploymentStage());
}

/**
 * The stage exactly as the deployment injected it — `undefined` when nothing arrived, and the raw
 * string when it names a stage the enum does not know.
 *
 * Reports absence as absence, where {@link deploymentStage} answers with its fallback. A display
 * that shows `Production` has to be able to say whether the deployment reported it or whether
 * nothing was injected; every other axis makes that distinction on its own.
 */
export function injectedDeploymentStage(): string | undefined {
  return readInjectedMeta(RuntimeContextMetaNames.deploymentStage);
}

/**
 * Whether this frontend is served on a development deployment — the local machine or the dev server.
 *
 * The predicate lives here rather than at each call site: a stage comparison spread across the
 * codebase is a stage comparison nobody can change.
 */
export function isDevelopmentStage(): boolean {
  return deploymentStage() === DeploymentStage.Development;
}

/**
 * Whether this frontend is served on the public demo deployment.
 */
export function isDemoDeployment(): boolean {
  return deploymentStage() === DeploymentStage.Demo;
}

/**
 * Narrows an injected string to the generated enum. Three cases, and only the first is the happy
 * path:
 *
 * - **a value the enum contains** — the deployment said what it is. Believe it.
 * - **a value the enum does not contain** — the deployment is misconfigured, or this bundle predates
 *   a stage the host was moved to. Falls back to `Production`, the most restrictive stage:
 *   `Development` unlocks developer-only UI, and an environment that reports an unknown stage is
 *   not one.
 * - **nothing injected** — decided by {@link runsUnderViteDevServer}, see there. The short version:
 *   a development machine gets `Development`, a built image gets `Production`.
 *
 * Never throws: a deployment detail must not take the app down.
 */
export function narrowDeploymentStage(raw: string | undefined): DeploymentStage {
  if (raw === undefined) {
    return runsUnderViteDevServer ? DeploymentStage.Development : DeploymentStage.Production;
  }

  return knownDeploymentStages.find((stage) => stage === raw) ?? DeploymentStage.Production;
}

/**
 * Whether this code runs under a Vite dev server or a vitest run, as opposed to a built bundle.
 *
 * This is the one place a build-time constant is still the right question. It does not decide the
 * stage — an injected value always wins — it only decides what *absence* means, and absence has two
 * very different causes:
 *
 * - **a development machine** (`pnpm dev`, `pnpm ladle`, vitest): a vitest run has no dev server to
 *   inject anything, and a dev server fills only the tags its HTML carries. `Development` is correct.
 * - **a built image whose HTML carries no `runtime-context.deploymentStage` meta tag yet**: most
 *   frontends are in this state. Assuming `Development` there would unlock developer-only UI — the
 *   form debug panel, the i18n pseudo-languages — on production deployments. `Production` is the safe
 *   read, and a frontend opts into the truth by adding the meta tag to its HTML.
 *
 * `import.meta.env.DEV` is `true` under both the Vite dev server and vitest, and `false` in anything
 * `vite build` or `ladle build` produced. The explicit assertion avoids pulling `vite/client`
 * ambient types into this commons package, which would mean adding vite as a devDependency just for
 * the type.
 */
const runsUnderViteDevServer: boolean = (import.meta as unknown as { env?: { DEV?: boolean } }).env?.DEV === true;

/**
 * Every stage the generated enum knows. Derived rather than listed, so a new
 * `it.neckar.runtime.context.DeploymentStage` constant needs no change here — a `switch` with a
 * `default` branch would swallow it without the compiler noticing.
 */
const knownDeploymentStages: readonly DeploymentStage[] = Object.values(DeploymentStage);
