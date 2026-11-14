import { createContext, useContext } from "react";

type OnLogoutFn = () => Promise<void>;

const LogoutContext = createContext<OnLogoutFn | undefined>(undefined);

export function LogoutContextProvider({
  onLogout,
  children,
}: {
  onLogout: OnLogoutFn | undefined;
  children: React.ReactNode;
}): React.ReactElement {
  return <LogoutContext value={onLogout}>{children}</LogoutContext>;
}

/**
 * Returns the onLogout function if configured, undefined otherwise.
 */
export function useOnLogout(): OnLogoutFn | undefined {
  return useContext(LogoutContext);
}
