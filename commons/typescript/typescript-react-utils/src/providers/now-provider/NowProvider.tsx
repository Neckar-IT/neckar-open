/**
 * Context that provides a function to return the current date, but also can return a specific date for tests
 * and Storybook stories
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
 * The fixed "now" of Storybook stories and tests: `<NowProvider value={nowMockedValue}>`.
 * App code reads the time through `useNow()` or `useGetNow()`.
 */
export const nowMockedValue: Date = new Date("2024-02-01T11:22:33.000Z");

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
  // The time, not the Date instance, keeps `getNow` stable when a caller passes a new Date per render;
  // each call returns a copy, so no caller can change the shared `nowMockedValue`.
  const fixedTime = value?.getTime();
  const getNow = useCallback(() => (fixedTime === undefined ? new Date() : new Date(fixedTime)), [fixedTime]);

  return <NowContext.Provider value={getNow}>{children}</NowContext.Provider>;
}

/**
 * Hooks
 */

/** The current date at render time. */
export function useNow(): Date {
  return useNowContext("useNow")();
}

/**
 * The function returning the current date, for a callback that needs the time when it runs.
 * Stays the same between renders while the time of `NowProvider`'s `value` stays the same.
 */
export function useGetNow(): () => Date {
  return useNowContext("useGetNow");
}

function useNowContext(hookName: string): () => Date {
  const getNow = useContext(NowContext);

  if (!getNow) {
    throw new Error(`${hookName} must be used within a NowProvider`);
  }

  return getNow;
}
