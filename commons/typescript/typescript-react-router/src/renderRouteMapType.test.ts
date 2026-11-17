/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { renderRouteMapType } from "./renderRouteMapType";

/**
 * Tests
 */

describe("renderRouteMapType", () => {
  it("should map a route without parameters to the no-params type", () => {
    const rendered = renderRouteMapType(["/customers"], { typeName: "AppRoutes", paramType: "Uuid" });

    expect(rendered).toBe(["// prettier-ignore", "export type AppRoutes = {", '  "/customers": NoParams;', "};", ""].join("\n"));
  });

  it("should map each path parameter to the configured parameter type", () => {
    const rendered = renderRouteMapType(["/customers/:customerId"], { typeName: "AppRoutes", paramType: "Uuid" });

    expect(rendered).toContain('  "/customers/:customerId": { customerId: Uuid };');
  });

  it("should list several path parameters in the order they appear in the pattern", () => {
    const rendered = renderRouteMapType(["/projects/:projectId/configurations/:configurationId"], {
      typeName: "AppRoutes",
      paramType: "Uuid",
    });

    expect(rendered).toContain('  "/projects/:projectId/configurations/:configurationId": { projectId: Uuid; configurationId: Uuid };');
  });

  it("should declare a repeated path parameter only once", () => {
    const rendered = renderRouteMapType(["/u/:id/m/:id"], { typeName: "AppRoutes", paramType: "Uuid" });

    expect(rendered).toContain('  "/u/:id/m/:id": { id: Uuid };');
  });

  it("should separate top level route groups by a blank line", () => {
    const rendered = renderRouteMapType(["/", "/customers", "/customers/:customerId", "/projects"], {
      typeName: "AppRoutes",
      paramType: "Uuid",
    });

    expect(rendered).toBe(
      [
        "// prettier-ignore",
        "export type AppRoutes = {",
        '  "/": NoParams;',
        "",
        '  "/customers": NoParams;',
        '  "/customers/:customerId": { customerId: Uuid };',
        "",
        '  "/projects": NoParams;',
        "};",
        "",
      ].join("\n"),
    );
  });

  it("should use the configured type name", () => {
    const rendered = renderRouteMapType(["/"], { typeName: "AdminRoutes", paramType: "Uuid" });

    expect(rendered).toContain("export type AdminRoutes = {");
  });
});
