/**
 * Use this component as a decorator of a story whose component uses a React Router hook.
 */

/**
 * Imports
 */

import { isDefined } from "@neckar-hub/typescript-utils/lang";
import type { PropsWithChildren, ReactElement } from "react";
import { MemoryRouter, Route, Routes, generatePath, useNavigate, useRouteError } from "react-router";
import type { StorybookStoryRoute } from "./storybookStoryRoute";

/**
 * Props
 */

type Props = PropsWithChildren<StorybookStoryRoute>;

/**
 * Component
 */

export function WithMemoryRouter({ path, url, params, children }: Props) {
  const pathTemplate = path ?? "/";
  const stringParams = Object.fromEntries(Object.entries(params ?? {}).map(([key, value]) => [key, String(value)]));
  const currentUrl = url ?? (path ? generatePath(path, stringParams) : "/");

  // Changing this key forces MemoryRouter to remount with fresh initialEntries
  const routerKey = `${pathTemplate}::${currentUrl}`;

  return (
    <MemoryRouter key={routerKey} initialEntries={[currentUrl]}>
      <Routes>
        <Route path={pathTemplate} element={<>{children}</>} errorElement={<RethrowError />} />
        <Route path="*" element={<BackLink />} />
      </Routes>
    </MemoryRouter>
  );
}

/**
 * Link back to the previous page
 */

function BackLink(): ReactElement {
  const navigate = useNavigate();

  return <button onClick={() => navigate(-1)}>Back</button>;
}

/**
 * Error Boundary that just rethrows the error
 */

function RethrowError() {
  const error = useRouteError();
  if (isDefined(error) && error instanceof Error) {
    throw error;
  }

  return null;
}
