import { endClientErrorReporter, getIdentitySpanProcessor } from "@neckar-hub/typescript-otel-tracing";
import { useEffect } from "react";
import { useUser } from "../context/UserContext";
import { readImpersonatedUserId } from "../impersonation/impersonationStorage";

/**
 * Sets the OTel identity (enduser.id) from the authenticated user, and none while the tab impersonates.
 * Ends `installClientErrorReporter`: from the login on errors go through `captureError`.
 *
 * Call this inside a component that is rendered after authentication, i.e. inside the auth provider.
 */
export function useOtelIdentity(): void {
  const user = useUser();
  const impersonating = readImpersonatedUserId() !== null;

  useEffect(() => {
    endClientErrorReporter();

    // While impersonating, the me answer names the target, not the signed-in user who acts; no identity beats a wrong one.
    if (impersonating) return;

    const processor = getIdentitySpanProcessor();
    if (processor == null) return;

    processor.setIdentity({
      userId: user.loginName,
    });

    return () => {
      processor.clearIdentity();
    };
  }, [user.loginName, impersonating]);
}
