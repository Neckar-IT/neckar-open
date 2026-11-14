import { useCallback, useState } from "react";

/** The logout of the login gateway of the host, which deletes the session, ends the Keycloak session and comes back at `/`. */
const ProxyLogoutPath = "/auth/logout";

/**
 * Signs out through the login gateway, which is the only holder of anything that survives a reload. A form POST navigates the whole
 * page and carries the `Origin` the gateway checks. `isPending` guards the button until the navigation happens and is never reset.
 */
export function useProxyLogout(): { logout: () => void; isPending: boolean } {
  const [isPending, setIsPending] = useState(false);

  const logout = useCallback(() => {
    setIsPending(true);
    const form = document.createElement("form");
    form.method = "post";
    form.action = ProxyLogoutPath;
    document.body.append(form);
    form.submit();
  }, []);

  return { logout, isPending };
}
