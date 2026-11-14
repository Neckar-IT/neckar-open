/**
 * Imports
 */

import { createContext, useContext, type PropsWithChildren } from "react";

/**
 * Constants
 */

/** Recipient of the support mail for every product that sets no `contactEmail` of its own. */
export const DefaultSupportContactEmail = "info@neckar.it";

/**
 * Context
 */

const SupportContactContext = createContext<string>(DefaultSupportContactEmail);

/**
 * Provider
 *
 * Always rendered by [GlobalErrorHandler]; without a `contactEmail` prop it provides [DefaultSupportContactEmail].
 */

export function SupportContactProvider(props: PropsWithChildren<{ contactEmail?: string | undefined }>) {
  return (
    <SupportContactContext.Provider value={props.contactEmail ?? DefaultSupportContactEmail}>
      {props.children}
    </SupportContactContext.Provider>
  );
}

/**
 * Hook
 *
 * Recipient of the support mail.
 */

export function useSupportContactEmail(): string {
  return useContext(SupportContactContext);
}
