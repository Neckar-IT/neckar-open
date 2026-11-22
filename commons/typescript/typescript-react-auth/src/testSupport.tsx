import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { UserContextProvider } from "./context/UserContext";

const defaultTenantId = "5b0c8d2e-0000-4000-8000-0000000000aa";

/**
 * Wrapper for `renderHook` and `render`; an app whose test setup mocks its capability hooks unmocks them first.
 * `tenantId` omitted puts the user into the tenant "Buchclub Tübingen", `null` makes a user without tenant.
 */
export function userWithCapabilities(
  capabilities: string[],
  options: { tenantId?: string | null } = {},
): ({ children }: { children: React.ReactNode }) => React.ReactElement {
  const tenantId = options.tenantId === undefined ? defaultTenantId : options.tenantId;
  const user: UserDetails = {
    id: "5b0c8d2e-0000-4000-8000-000000000001",
    loginName: "emma.woodhouse",
    firstName: "Emma",
    lastName: "Woodhouse",
    email: "emma.woodhouse@example.com",
    avatarUrl: null,
    environment: [],
    tenant: tenantId === null ? null : { id: tenantId, name: "Buchclub Tübingen" },
    capabilities,
    roleLabel: null,
  };
  return ({ children }) => <UserContextProvider user={user}>{children}</UserContextProvider>;
}
