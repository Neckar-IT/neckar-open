/**
 * Re-Export necessary react-router exports
 */

export { BrowserRouter, MemoryRouter, matchPath, Outlet, Route, Routes, useLocation, useMatch, useParams } from "react-router";

export { createLink, createLinkPropsGenerator } from "./createLink";
export { createUrlGenerator } from "./createUrlGenerator";
export { createUseNavigate } from "./createUseNavigate";
export { createUseNavigateBack } from "./createUseNavigateBack";
export { createUsePathParams } from "./createUsePathParams";
export { type LinkPropsPrimitive, type NavigatePropsPrimitive, type NoParams } from "./types";

/**
 * Derive the route map type from a JSX route tree
 */

export { extractRoutePatterns } from "./extractRoutePatterns";
export { renderRouteMapType, type RouteMapTypeOptions } from "./renderRouteMapType";
