/**
 * Imports
 */

import { QueryClientProvider, type QueryClient } from "@tanstack/react-query";
import { lazy, Suspense, type PropsWithChildren, type ReactElement } from "react";

/**
 * React Query Devtools — lazy-loaded in dev, omitted in production.
 *
 * A static `import { ReactQueryDevtools } from "@tanstack/react-query-devtools"` pulls the
 * package into the initial module graph, and Vite's pre-bundler then encounters a nested
 * dynamic `import("./query-devtools-…js")` inside the pre-bundled file that it cannot crawl.
 * It lazily re-optimizes the transitive `@tanstack/query-devtools` dep on first mount,
 * rotates the browser hash, and leaves the already-loaded tab pointing at a stale URL —
 * which surfaces as `TypeError: error loading dynamically imported module` on every reload.
 *
 * Lazy-loading it from our side makes the whole devtools package a real React.lazy chunk
 * and gates it behind `import.meta.env.DEV`, so the production build drops it entirely.
 */
// `import.meta.env.DEV` is populated in Vite-compiled frontends and undefined in Vitest/Node
// contexts. The explicit assertion avoids pulling `vite/client` ambient types into this shared
// commons package (which would require adding vite as a devDependency just for the type).
const importMetaEnv = (import.meta as unknown as { env?: { DEV?: boolean } }).env;
const ReactQueryDevtoolsLazy =
  importMetaEnv?.DEV === true
    ? lazy(() => import("@tanstack/react-query-devtools").then((module) => ({ default: module.ReactQueryDevtools })))
    : null;

/**
 * Query Client Provider (includes react-query devtools in dev)
 */

type QueryClientProviderWithDevtoolsProps = PropsWithChildren<{
  client: QueryClient;
}>;

export function QueryClientProviderWithDevtools({ client, children }: QueryClientProviderWithDevtoolsProps): ReactElement {
  return (
    <QueryClientProvider client={client}>
      {children}
      {ReactQueryDevtoolsLazy !== null && (
        <Suspense fallback={null}>
          <ReactQueryDevtoolsLazy />
        </Suspense>
      )}
    </QueryClientProvider>
  );
}
