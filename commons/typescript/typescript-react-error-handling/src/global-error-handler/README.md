# Global Error Handler

Our goal is to make error handling in react applications as easy as possible.

## Usage

To use the global error handler, you need to wrap your application with the `GlobalErrorHandler` component and use the
`RouterErrorElement` component as the errorElement in the topmost route of your react router configuration.

```jsx
// src/main.tsx

import {
  GlobalErrorHandler,
  RouterErrorElement
} from '@neckar-hub/typescript-react-error-handling/global-error-handler'

const router = createBrowserRouter([
  {
    path: "/",
    errorElement: <RouterErrorElement />,
    element: <App />,
    children: [ ... ]
  }
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GlobalErrorHandler>
      {/* other providers go here */}
      <RouterProvider router={router} />
    </GlobalErrorHandler>
  </StrictMode>
)
```

### Support Contact and Trace Links

Two optional props of `GlobalErrorHandler` complete the error pages.

- `contactEmail` addresses the mail the "Contact Support" button opens. The mail carries the error information the copy button next to it copies, truncated to the length mail clients accept. Without `contactEmail` it goes to `info@neckar.it`.
- `observability` turns trace IDs into Grafana-Tempo links: pass `grafanaTraceObservabilityConfig()` from `@neckar-hub/typescript-otel-tracing`.

```jsx
<GlobalErrorHandler contactEmail={contactEmail} observability={grafanaTraceObservabilityConfig()}>
```

### Custom Errors and Fallback Pages

You can define custom error pages for specific errors and pass them in as conditions to `GlobalErrorHandler` and `RouterErrorElement`.

```jsx
// src/main.tsx

import {
  type ErrorHandler,
  GlobalErrorHandler,
  RouterErrorElement
} from '@neckar-hub/typescript-react-error-handling/global-error-handler'

const customErrorHandler: ErrorHandler = (error, errorCaughtBy) => {
  if (isCustomError(error)) {
    return <CustomErrorPage />;
  }
  return null;
}

const router = createBrowserRouter([
  {
    path: "/",
    errorElement: <RouterErrorElement errorHandlers={[ customErrorHandler ]} />,
    element: <App />,
    children: [ ... ]
  }
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GlobalErrorHandler errorHandlers={[ customErrorHandler ]}>
      {/* other providers go here */}
      <RouterProvider router={router} />
    </GlobalErrorHandler>
  </StrictMode>
)
```

## Built-in Handlers

The reaction per error class is decided in ADL 0105, in code by `reactionToFailure` of `typescript-react-query`; the reaction per status and request: `docs/react/http-status-handling.html` in the monorepo. `GlobalErrorFallback` runs the handlers of the `errorHandlers` prop first, then the built-in handlers; the built-in handlers take disjoint reactions, and `unknownErrorHandler` takes the rest.

| Handler                          | Reaction of `reactionToFailure(error, "query")`                                                            | Page                                                                                                                   |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `versionMismatchErrorHandler`    | `versionMismatch`                                                                                          | „Neue Version verfügbar!“, reloads after 60 seconds                                                                    |
| `serviceUnavailableErrorHandler` | `serviceUnavailablePage`                                                                                   | `ServiceUnavailablePage`: text of the polled `ServiceAvailability`, else of the failed request; back and retry buttons |
| `missingCapabilityErrorHandler`  | `permissionDenied`                                                                                         | `PermissionDeniedPage` naming the capability from `attributes`                                                         |
| `applicationErrorHandler`        | `errorPage` or `sessionExpired` with an `ExpectedApplicationError` or `UnexpectedApplicationError` payload | `UnexpectedErrorPage` with `errorCode` or exception type, message, trace link and request details                      |
| `unknownErrorHandler`            | everything else                                                                                            | `UnexpectedErrorPage` with message and browser stack trace                                                             |

`GlobalErrorFallback` reads a JSON payload out of a Blob body before the first handler runs.

`ServiceUnavailablePage` recovers depending on where it renders:

- **Below `AppConnectivityProvider`:** `ServiceUnavailableBoundary` renders `ServiceUnavailablePage` and fetches the failed queries again in each recovery round of the health poll.
- **Under `RouterErrorElement`, in a data router from `createBrowserRouter`:** each recovery round and the retry button reset the failed queries, navigate to the same location and revalidate.
- **Under `MeSessionScreens`:** `useMeSession` fetches `/me` again every 5 seconds.
- **In `GlobalErrorHandler`:** the retry button reloads the page.

A same-origin 401 of the health poll ends the outage, so the recovery round remounts the auth provider and `SessionExpiredScreen` replaces `ServiceUnavailablePage`.

`GlobalErrorListener` ignores an unhandled rejection that a toast, an alert or the auth provider answers (`notExecuted`, `unknownOutcome`, `cancelled`, `sessionExpired`).

A same-origin 401 reaches this chain only in an app without `createQueryClient({ unauthorizedEndsSession: true })`. `typescript-react-auth` answers a 401 on `/me`: `KeycloakAuthProvider` and `ProxyAuthProvider` show `SessionExpiredScreen`, `CredentialsAuthProvider` shows its login, `TokenAuthProvider` logs in with the token. The offline notice comes from `AppConnectivityProvider`, which reads `window.navigator.onLine`; that flag reports the network connection, not the reachability of the internet.
