/**
 * Imports
 */

import { Route, Routes } from "react-router";
import { describe, expect, it } from "vitest";
import { extractRoutePatterns } from "./extractRoutePatterns";

/**
 * Tests
 */

describe("extractRoutePatterns", () => {
  it("should map an index route to the path of its parent", () => {
    const routes = (
      <Routes>
        <Route path="/" element={<div />}>
          <Route index={true} element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/"]);
  });

  it("should join nested paths", () => {
    const routes = (
      <Routes>
        <Route path="/" element={<div />}>
          <Route path="customers">
            <Route index={true} element={<div />} />
            <Route path=":customerId" element={<div />} />
          </Route>
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/", "/customers", "/customers/:customerId"]);
  });

  it("should keep a route that carries an element and children", () => {
    const routes = (
      <Routes>
        <Route path="projects/:projectId" element={<div />}>
          <Route path="edit" element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/projects/:projectId", "/projects/:projectId/edit"]);
  });

  it("should split a path that spans multiple segments", () => {
    const routes = (
      <Routes>
        <Route path="electro">
          <Route path="projects/:projectId/configurations/:configurationId" element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/electro/projects/:projectId/configurations/:configurationId"]);
  });

  it("should keep a route that renders through the Component prop", () => {
    const routes = (
      <Routes>
        <Route path="dashboard" Component={() => <div />} />
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/dashboard"]);
  });

  it("should skip a grouping route that renders nothing", () => {
    const routes = (
      <Routes>
        <Route path="settings">
          <Route path="user" element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/settings/user"]);
  });

  it("should skip a pathless layout route but keep its children", () => {
    const routes = (
      <Routes>
        <Route element={<div />}>
          <Route path="dashboard" element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/dashboard"]);
  });

  it("should skip splat routes", () => {
    const routes = (
      <Routes>
        <Route path="dashboard" element={<div />} />
        <Route path="files/*" element={<div />} />
        <Route path="*" element={<div />} />
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/dashboard"]);
  });

  it("should treat a child path starting with a slash as absolute", () => {
    const routes = (
      <Routes>
        <Route path="pv">
          <Route path="/admin/impersonation" element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/admin/impersonation"]);
  });

  it("should collect routes that arrive as a fragment from another module", () => {
    const employeeRoutes = (
      <Route path="employees">
        <Route index={true} element={<div />} />
        <Route path=":employeeId" element={<div />} />
      </Route>
    );

    const routes = (
      <Routes>
        <Route path="/" element={<div />}>
          {employeeRoutes}
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/", "/employees", "/employees/:employeeId"]);
  });

  it("should report every pattern only once", () => {
    const routes = (
      <Routes>
        <Route path="customers" element={<div />}>
          <Route index={true} element={<div />} />
        </Route>
      </Routes>
    );

    expect(extractRoutePatterns(routes)).toEqual(["/customers"]);
  });
});
