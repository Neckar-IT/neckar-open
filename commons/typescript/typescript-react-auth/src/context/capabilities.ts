/**
 * Imports
 */

import { useUser } from "./UserContext";

/**
 * Capabilities every product shares, mirroring `it.neckar.open.authorization.CoreCapability`.
 * A product declares its own keys next to these, in the same `<namespace>:<action>` form.
 */
export const CoreCapability = {
  ReadData: "core:read-data",
  WriteData: "core:write-data",
  DeleteData: "core:delete-data",
  ManageMembers: "core:manage-members",
  ManageTenant: "core:manage-tenant",
  OperateProduct: "core:operate-product",
  ImpersonateUsers: "core:impersonate-users",
} as const;

/**
 * The capabilities of the current user, as the `me` response delivered them. Empty in a product
 * that grants none — every gate below then simply stays closed.
 */
export function useCapabilities(): string[] {
  return useUser().capabilities ?? [];
}

/**
 * Whether the current user holds [capability]. The endpoints check the same key, so a visible
 * action and an accepted request cannot disagree.
 */
export function useHasCapability(capability: string): boolean {
  return useCapabilities().includes(capability);
}

/**
 * The tenant the current user acts for. Null in a product without tenants.
 *
 * Comes from the server rather than from the token, so no frontend decodes a JWT or compares
 * against a hardcoded tenant id.
 */
export function useTenantId(): string | null {
  return useUser().tenantId ?? null;
}
