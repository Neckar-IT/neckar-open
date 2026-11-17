/**
 * Vite optimizeDeps entries to pre-bundle the React Query Devtools and its
 * transitive UI package up front.
 *
 * Why this is needed
 * ------------------
 * `QueryClientProviderWithDevtools` (in `@neckar-hub/typescript-react-query`) loads
 * `@tanstack/react-query-devtools` via `React.lazy(() => import(...))`. The devtools package
 * itself then does a nested dynamic import of `@tanstack/query-devtools` (the UI shell) at
 * component mount.
 *
 * Vite's pre-bundler cannot crawl nested dynamic imports inside already-pre-bundled deps,
 * so on first mount it lazily re-optimizes `@tanstack/query-devtools`. That rotates the
 * browser hash and leaves the already-loaded tab pointing at stale module URLs. The next
 * render of `ReactQueryDevtools` hits a stale `chunk-*.js` that no longer matches the
 * fresh `QueryClientContext` instance — `useContext(QueryClientContext)` returns `null` —
 * the `GlobalErrorHandler` catches the resulting `TypeError` and the user sees a blank
 * page until they full-reload (which clears the stale URLs).
 *
 * The symptom shows up after Keycloak roundtrips and on HMR refreshes during an active
 * session, intermittently and dev-only.
 *
 * Listing both packages in `optimizeDeps.include` forces Vite to pre-bundle them at
 * startup, keeping the hash stable across reloads.
 *
 * Required setup in each consuming frontend
 * -----------------------------------------
 * Both packages must be listed as direct `devDependencies` of the consuming frontend's
 * `package.json`. They are devDependencies of `typescript-react-query`, but pnpm strict
 * mode hides transitive devDeps from the consumer's Vite resolver, so `optimizeDeps.include`
 * cannot reach them through the workspace symlink alone. Without the direct listing Vite
 * logs `Failed to resolve dependency: @tanstack/query-devtools, present in client
 * 'optimizeDeps.include'` at startup and the bug returns.
 */
export function reactQueryDevtoolsPreBundleIncludes(): string[] {
  return ["@tanstack/react-query-devtools", "@tanstack/query-devtools"];
}
