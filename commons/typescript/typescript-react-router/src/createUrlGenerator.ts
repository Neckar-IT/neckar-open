/**
 * Imports
 */

import type { LinkPropsPrimitive, NoParams } from "./types";
import { buildUrl } from "./utils";

/**
 * Factory function for creating a type safe url (requires AppRoutes generic type)
 *
 * Example Usage:
 * ```
 * interface AppRoutes {
 *   "/": NoParams;
 *   "/chickens": NoParams;
 *   "/chickens/:chickenId": { chickenId: string };
 * }
 *
 * const generateUrl = createUrlGenerator<AppRoutes>()
 * ```
 */

export function createUrlGenerator<AppRoutes extends Record<string, Record<string, string> | NoParams>>() {
  return function generateUrl(link: LinkPropsPrimitive<AppRoutes>): string {
    return buildUrl(link);
  };
}
