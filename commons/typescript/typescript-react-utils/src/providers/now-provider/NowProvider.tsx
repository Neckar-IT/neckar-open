/**
 * Context that provides a function to return the current date, but also can return a specific date for tests
 * and ladle stories
 *
 * Usage:
 * const now = useNow()
 * const today = now.toISOString()
 */

/**
 * Imports
 */

import type { PropsWithChildren } from "react";
import { createContext, useCallback, useContext } from "react";

/**
 * Context
 */

const NowContext = createContext<(() => Date) | null>(null);

/**
 * Provider
 */

type ProviderProps = PropsWithChildren<{
  value?: Date;
}>;

export function NowProvider({ children, value }: ProviderProps) {
  // If `value` is provided, "now" is fixed to that date (test/story usage).
  // Otherwise return the current time on each call.
  const getNow = useCallback(() => value ?? new Date(), [value]);

  return <NowContext.Provider value={getNow}>{children}</NowContext.Provider>;
}

/**
 * Hook
 */

export function useNow() {
  const getNow = useContext(NowContext);

  if (!getNow) {
    throw new Error("useNow must be used within a NowProvider");
  }

  const now = getNow();
  return now;
}
