/**
 * Imports
 */

import { useNavigate } from "react-router";
import type { NavigatePropsPrimitive, NoParams } from "./types";
import { buildUrl } from "./utils";

/**
 * Factory function to create a typed useNavigate hook (requires AppRoutes generic type)
 *
 * Example Usage:
 * ```
 * interface AppRoutes {
 *   "/": NoParams;
 *   "/chickens": NoParams;
 *   "/chickens/:chickenId": { chickenId: string };
 * }
 *
 * const useNavigate = createUseNavigate<AppRoutes>()
 *
 * // Navigate with search params:
 * navigate({ to: "/chickens", searchParams: { sort: "name" } })
 * ```
 */

export function createUseNavigate<AppRoutes extends Record<string, Record<string, string> | NoParams>>() {
  return function useTypedNavigate() {
    const navigate = useNavigate();

    return (link: NavigatePropsPrimitive<AppRoutes>) => {
      const url = buildUrl(link);
      void navigate(url);
    };
  };
}
