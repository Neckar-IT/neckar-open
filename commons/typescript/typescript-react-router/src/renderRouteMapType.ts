/**
 * Imports
 */

import { extractParamNames } from "./utils";

/**
 * Options for the rendered route map type
 */

export type RouteMapTypeOptions = {
  /** Name of the generated type, e.g. `AppRoutes` */
  typeName: string;
  /** Type every path parameter is declared with, e.g. `Uuid` */
  paramType: string;
};

/**
 * Render the route patterns as the route map type that drives type safe routing.
 *
 * Routes are grouped by their first path segment, separated by a blank line, in the order they were collected
 * from the JSX route tree. The declaration carries a `prettier-ignore` because a route with several parameters
 * exceeds the configured print width and would otherwise be wrapped across lines.
 *
 * Only the type declaration is rendered — the file the declaration ends up in has to import `NoParams` from this
 * library and whatever `paramType` names.
 */

export function renderRouteMapType(patterns: string[], options: RouteMapTypeOptions): string {
  const lines: string[] = ["// prettier-ignore", `export type ${options.typeName} = {`];

  let previousGroup: string | undefined = undefined;
  for (const pattern of patterns) {
    const group = firstSegment(pattern);
    if (previousGroup !== undefined && group !== previousGroup) {
      lines.push("");
    }
    previousGroup = group;

    lines.push(`  "${pattern}": ${renderParams(pattern, options.paramType)};`);
  }

  lines.push("};", "");

  return lines.join("\n");
}

function renderParams(pattern: string, paramType: string): string {
  const paramNames = Array.from(new Set(extractParamNames(pattern)));
  if (paramNames.length === 0) return "NoParams";

  return `{ ${paramNames.map((paramName) => `${paramName}: ${paramType}`).join("; ")} }`;
}

function firstSegment(pattern: string): string {
  return pattern.split("/").find((segment) => segment.length > 0) ?? "";
}
