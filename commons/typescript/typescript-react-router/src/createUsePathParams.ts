/**
 * Imports
 */

import { useParams } from "react-router";
import type { NoParams } from "./types";
import { extractParamNames } from "./utils";

/**
 * Factory function to create a typed usePathParam hook (requires AppRoutes generic type)
 *
 * Example Usage:
 * ```
 * interface AppRoutes {
 *   "/": NoParams;
 *   "/chickens": NoParams;
 *   "/chickens/:chickenId": { chickenId: string };
 * }
 *
 * const usePathParams = createUsePathParams<AppRoutes>()
 * ```
 */

export function createUsePathParams<AppRoutes extends Record<string, Record<string, string> | NoParams>>() {
  type ExtractParams<T extends keyof AppRoutes> = AppRoutes[T] extends NoParams ? NoParams : AppRoutes[T];

  return function usePathParams<T extends keyof AppRoutes>(pattern: T): ExtractParams<T> {
    const routeParams = useParams();
    const names = extractParamNames(String(pattern));

    if (names.length === 0) {
      throw new Error(`The provided pattern '${String(pattern)}' does not contain any route parameters.`);
    }

    const collected: Record<string, string> = {};
    for (const n of names) {
      const val = routeParams[n];

      if (val === undefined) {
        throw new Error(`Missing required route param '${n}' for pattern '${String(pattern)}'`);
      }

      collected[n] = val;
    }
    return collected as unknown as ExtractParams<T>;
  };
}
