/**
 * Imports
 */

import { isValidElement, type ReactNode } from "react";
import { Route } from "react-router";

/**
 * Props of a `<Route>` element, as far as the route patterns are concerned
 */

type RouteElementProps = {
  path?: string;
  index?: boolean;
  element?: ReactNode;
  Component?: unknown;
  children?: ReactNode;
};

/**
 * Collect the URL patterns of every navigable route in a JSX route tree.
 *
 * A route is navigable when it renders something (`element` or `Component`) and is addressable: an index route
 * (which resolves to its parent's path), or a route with its own path. Grouping routes that render nothing and
 * pathless layout routes are traversed but not reported, and splat routes are left out because they are no link
 * target. Route elements are recognized by identity with react-router's `Route`.
 */

export function extractRoutePatterns(routes: ReactNode): string[] {
  const patterns: string[] = [];
  collectRoutePatterns(routes, "/", patterns);

  return Array.from(new Set(patterns)).filter((pattern) => !pattern.includes("*"));
}

function collectRoutePatterns(node: ReactNode, basePath: string, patterns: string[]): void {
  if (Array.isArray(node)) {
    for (const child of node as ReactNode[]) {
      collectRoutePatterns(child, basePath, patterns);
    }
    return;
  }

  if (!isValidElement(node)) return;

  const props = node.props as RouteElementProps;

  // <Routes>, fragments and wrapper elements do not contribute a path segment
  if (node.type !== Route) {
    collectRoutePatterns(props.children, basePath, patterns);
    return;
  }

  const fullPath = props.index === true ? basePath : joinPaths(basePath, props.path);
  const rendersSomething = props.element !== undefined || props.Component !== undefined;

  if (rendersSomething && (props.index === true || props.path !== undefined)) {
    patterns.push(fullPath);
  }

  collectRoutePatterns(props.children, fullPath, patterns);
}

function joinPaths(basePath: string, path: string | undefined): string {
  if (path === undefined) return basePath;

  return normalizePath(path.startsWith("/") ? path : `${basePath}/${path}`);
}

function normalizePath(path: string): string {
  const segments = path.split("/").filter((segment) => segment.length > 0);

  return segments.length === 0 ? "/" : `/${segments.join("/")}`;
}
