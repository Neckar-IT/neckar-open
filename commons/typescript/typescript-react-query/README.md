# typescript-react-query: Customized @tanstack/react-query Library

This package mainly reexports the [`@tanstack/react-query`](https://tanstack.com/query/latest) library with some custom
configurations and enhancements. It also contains utility hooks and components to simplify the usage of React Query in
TypeScript projects.

## Customizations

- QueryClient (`createQueryClient`):
  - queries and mutations throw on error by default (`throwOnError`); a cancelled mutation does not throw
  - queries retry twice instead of the React Query default of three times
  - all queries are reset after every _successful_ mutation. A mutation resolving to a `bad/*` `RestResponse` does not
    count as success and leaves the cache untouched, unless its type is listed in
    `meta.resetQueriesOnBadResponses` of the mutation (e.g. `["bad/NotFound"]` on a delete). Opt out per mutation with
    `meta.skipGlobalQueryResets = true`, or per query with `meta.preventBeingResetByMutations = true`.
  - `onMutationError` receives every failed mutation at the `MutationCache` level, even when the caller discards the
    returned promise
  - `unauthorizedEndsSession: true` takes a same-origin HTTP 401 (`isSameOriginUnauthorized`) out of retry,
    render-throw and `onMutationError`; `ProxyAuthProvider` of `typescript-react-auth` shows its sign-in-again screen.
    A query with its own `throwOnError: true` and `useSuspenseQuery` still throw that 401 to the error boundary.

- useQuery:
  - renamed `data` to `response` to avoid confusion with the `data` property of the response object
  - status: `QueryStatus.Pending` | `QueryStatus.Fetching` | `QueryStatus.Success`. There is no error status —
    `throwOnError` means errors reach the surrounding error boundary instead of the result object. A same-origin
    401 under `unauthorizedEndsSession` keeps the last state while the auth provider replaces the page.

## Utilities

### QueryClientProviderWithDevtools

This version of the `QueryClientProvider` includes React Query Devtools. They are lazy-loaded and only mounted when
`import.meta.env.DEV` is true, so the production build drops them entirely. Otherwise it can be used like the standard
`QueryClientProvider`.

### Query hooks for special use cases

- `useQueryForStaticData`
  This hook is designed for fetching data, that does not change often. It refetches on mount and on window focus.

- `useQueryForTableData`
  This hook is optimized for fetching data that will be displayed in a table or other list-like structures. The data
  will be refetched every 30 seconds. On a changed query parameter (filter, pagination) the query goes back to
  `Pending`, so the UI can show a skeleton.

- `useQueryForSearchResults`
  This hook is designed for type-ahead search. It keeps the previous results while the new search text is loading, so
  the status goes to `Fetching` instead of `Pending` and the result list never disappears while typing.

- `useQueryForDynamicData`
  This hook will refetch the data in small intervals (default: 1 second) and can be used for polling data from the
  backend.

- `useQueryForOneOffData`
  This hook is designed for data that is needed only once. It fetches on mount and drops the response from the cache
  as soon as the hook unmounts.

### useFileUploadMutation

Wraps `useMutation` for the upload of one file and reports the progress of the request body.

```ts
const upload = useFileUploadMutation({
  upload: (file, config) => putFile(id, file, { ...config, headers: { "Content-Type": file.type } }),
});
upload.mutate(file);
// upload.progress: idle → indeterminate → uploading 0.01 → … → uploading 0.99 → processing → idle
```

- `upload` receives `signal` and `onUploadProgress` as request config (an `AxiosRequestConfig` subset, as orval generated functions expect it).
- `progress` is an `UploadProgress`: `{ kind: "idle" }` without a running upload, `{ kind: "indeterminate" }` from the start until the first event carrying `progress` arrives (for good if the body length is unknown), `{ kind: "uploading", fraction }` with `fraction` in `0..<1` in whole percent while the body is sent, `{ kind: "processing" }` from the last sent byte until the response arrives. A progress bar shows `indeterminate` and `processing` without a fraction.
- `abort()` cancels the running request; the mutation ends in `isError`.
- `retry` is `0`. One instance carries one upload; parallel uploads need one instance each.
- `processing` means the body is sent. Only `onSuccess` confirms the upload, and a buffering proxy can make `progress` jump to `processing` early.
