/**
 * Imports
 */

import { readInjectedMeta } from "../serve-time-injection";
import { RuntimeContextMetaNames } from "./runtimeContextMeta";

/**
 * Hostname of the deployment this frontend is served on — `undefined` when nothing was injected.
 *
 * Stays a plain string where the two other axes narrow to an enum: `it.neckar.runtime.context.Hostname`
 * is a Kotlin value class over a string, so there is no set of known values to narrow against, and no
 * hostname would be a safe fallback.
 */
export function serviceHost(): string | undefined {
  return readInjectedMeta(RuntimeContextMetaNames.serviceHost);
}
