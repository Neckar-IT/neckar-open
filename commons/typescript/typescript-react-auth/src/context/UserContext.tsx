import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { createContext, useContext } from "react";

const UserContext = createContext<UserDetails | undefined>(undefined);

export function UserContextProvider({ user, children }: { user: UserDetails; children: React.ReactNode }): React.ReactElement {
  return <UserContext value={user}>{children}</UserContext>;
}

export function useUser(): UserDetails {
  const user = useContext(UserContext);

  if (user === undefined) {
    throw new Error("`useUser` hook must be used within an Auth Context!");
  }

  return user;
}
