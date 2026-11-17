import { CoreCapability, useHasCapability } from "../context/capabilities";
import { readImpersonatedUserId } from "./impersonationStorage";

/**
 * Whether the signed-in user may start an impersonation in this tab. False while the tab already impersonates: a second pick would
 * silently replace the user this tab acts as.
 */
export function useMayImpersonate(): boolean {
  return useHasCapability(CoreCapability.ImpersonateUsers) && readImpersonatedUserId() === null;
}
