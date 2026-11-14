/**
 * Imports
 */

import { type Link as RouterLink } from "react-router";

/**
 * Type for a route that does not have any parameters
 */

export type NoParams = Record<string | number | symbol, never>;

/**
 * Link Props
 */

export type LinkPropsPrimitive<
  AppRoutes extends Record<string, Record<string, string> | NoParams>,
  T extends keyof AppRoutes = keyof AppRoutes, // <-- default: all keys
> = {
  [K in T]: AppRoutes[K] extends NoParams
    ? Omit<React.ComponentProps<typeof RouterLink>, "to"> & {
        to: K;
        params?: never;
        searchParams?: URLSearchParams | Record<string, string | undefined>;
      }
    : Omit<React.ComponentProps<typeof RouterLink>, "to"> & {
        to: K;
        params: AppRoutes[K];
        searchParams?: URLSearchParams | Record<string, string | undefined>;
      };
}[T];

/**
 * Props for typed navigation (without Link component props)
 */

export type NavigatePropsPrimitive<
  AppRoutes extends Record<string, Record<string, string> | NoParams>,
  T extends keyof AppRoutes = keyof AppRoutes,
> = {
  [K in T]: AppRoutes[K] extends NoParams
    ? { to: K; params?: never; searchParams?: URLSearchParams | Record<string, string | undefined> }
    : { to: K; params: AppRoutes[K]; searchParams?: URLSearchParams | Record<string, string | undefined> };
}[T];
