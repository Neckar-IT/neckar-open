/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, type QueryClient, QueryClientProvider, useMutation, useQuery } from "@neckar-hub/typescript-react-query";
import type { MeResponse, MeResponseGoodWithType } from "@neckar-hub/typescript-utils/openapi-types";
import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, AxiosHeaders, type AxiosInstance } from "axios";
import { Component, type PropsWithChildren, type ReactElement, type ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getMeQueryKey } from "./hooks/useGetMeQuery";
import type { ProxyAuthConfig } from "./ProxyAuthConfig";
import { ProxyAuthProvider } from "./ProxyAuthProvider";
import { createUnauthorizedHttpClient } from "./testing/createUnauthorizedHttpClient";

const meResponse: MeResponseGoodWithType = {
  type: "good",
  data: {
    id: "00000000-0000-0000-0000-000000000001",
    email: "test-user@email.com",
    firstName: "Test",
    lastName: "User",
    loginName: "test-user",
    environment: [],
    avatarUrl: null,
    tenantId: null,
    capabilities: [],
    roleLabel: null,
  },
};

function httpError(status: number): AxiosError {
  return new AxiosError("failed", undefined, undefined, undefined, {
    status,
    statusText: "",
    data: {},
    headers: {},
    config: { headers: new AxiosHeaders() },
  });
}

function renderProvider(config: ProxyAuthConfig): QueryClient {
  const queryClient = createQueryClient();
  // Retries would put every failure assertion three seconds out; under test is the screen per
  // outcome, not the retry policy. The remaining defaults stay; unauthorizedEndsSession is off, as in a product without it.
  const defaults = queryClient.getDefaultOptions();
  queryClient.setDefaultOptions({ ...defaults, queries: { ...defaults.queries, retry: false } });

  render(
    <TestErrorBoundary>
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <ProxyAuthProvider config={config}>
            <div>Application</div>
          </ProxyAuthProvider>
        </QueryClientProvider>
      </I18nProvider>
    </TestErrorBoundary>,
  );
  return queryClient;
}

class TestErrorBoundary extends Component<PropsWithChildren, { failed: boolean }> {
  override state = { failed: false };

  static getDerivedStateFromError(): { failed: boolean } {
    return { failed: true };
  }

  override render(): ReactNode {
    return this.state.failed ? <div>Error boundary</div> : this.props.children;
  }
}

/**
 * Renders the provider the way patterns does: the error boundary above it, the query client with
 * `unauthorizedEndsSession` and its real retry policy; `retryDelay: 0` keeps the retries instant.
 */
function renderApplication(application: ReactElement, onMutationError: (error: unknown) => void = () => {}): void {
  const queryClient = createQueryClient({ onMutationError, unauthorizedEndsSession: true });
  const defaults = queryClient.getDefaultOptions();
  queryClient.setDefaultOptions({ ...defaults, queries: { ...defaults.queries, retryDelay: 0 } });

  render(
    <TestErrorBoundary>
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <ProxyAuthProvider config={{ onGetMe: () => Promise.resolve<MeResponse>(meResponse) }}>{application}</ProxyAuthProvider>
        </QueryClientProvider>
      </I18nProvider>
    </TestErrorBoundary>,
  );
}

function BookPage(props: { url: string; httpClient: AxiosInstance }): ReactElement {
  const book = useQuery({ queryKey: ["book", props.url], queryFn: () => props.httpClient.get(props.url) });
  return <div>Book {book.status}</div>;
}

function SaveButton(props: { httpClient: AxiosInstance }): ReactElement {
  const save = useMutation({ mutationFn: () => props.httpClient.put("/api/books/42") });
  return <button onClick={() => save.mutate()}>Save</button>;
}

/**
 * Tests
 */

describe("ProxyAuthProvider", () => {
  let replacedLocation = false;

  /** jsdom's `location` is an accessor; replacing it turns it into a data property for good. */
  function replaceLocation(replacement: { assign: () => void; reload: () => void }): void {
    replacedLocation = true;
    // href and origin stay, because the 401 counts as one of the page's own origin only against them.
    const { href, origin } = window.location;
    Object.defineProperty(window, "location", { value: { href, origin, ...replacement }, writable: true, configurable: true });
  }

  const originalLocation = window.location;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Restored here rather than at the end of a test so a failing assertion does not leak into the next one.
  afterEach(() => {
    vi.restoreAllMocks();
    if (replacedLocation) {
      Object.defineProperty(window, "location", { value: originalLocation, writable: true, configurable: true });
      replacedLocation = false;
    }
  });

  it("renders the application once the identity arrives, without any login of its own", async () => {
    // The page was served behind the login gateway, so the session already exists when the
    // provider mounts: GET /me is the only request it makes.
    const onGetMe = vi.fn(() => Promise.resolve<MeResponse>(meResponse));

    renderProvider({ onGetMe });

    expect(await screen.findByText("Application")).toBeInTheDocument();
    expect(onGetMe).toHaveBeenCalledTimes(1);
  });

  it("offers a fresh sign-in through the gateway when the backend answers 401", async () => {
    // The edge session ended while the app was open. The app holds no token it could renew, so the
    // only way on is a navigation the gateway answers with its redirect to /auth/login.
    const reload = vi.fn();
    const assign = vi.fn();
    replaceLocation({ assign, reload });

    renderProvider({ onGetMe: () => Promise.reject(httpError(401)) });

    expect(await screen.findByText("Session expired")).toBeInTheDocument();
    expect(screen.queryByText("Application")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Sign in again" }));

    expect(reload).toHaveBeenCalledTimes(1);
    // A reload keeps the current URL; a navigation elsewhere would lose the page the user is on.
    expect(assign).not.toHaveBeenCalled();
  });

  it.each([301, 404, 500])("hands HTTP %i to the error boundary", async (status) => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    renderProvider({ onGetMe: () => Promise.reject(httpError(status)) });

    expect(await screen.findByText("Error boundary")).toBeInTheDocument();
  });

  it("shows `ServiceUnavailablePage` when no response arrives at all", async () => {
    // The dropped-connection case: AxiosError without a response, which is what an offline browser produces.
    renderProvider({ onGetMe: () => Promise.reject(new AxiosError("network down")) });

    expect(await screen.findByText("No connection to the application.")).toBeInTheDocument();
  });

  it("shows `ServiceUnavailablePage` for a fetch() network error of the product's own onGetMe", async () => {
    renderProvider({ onGetMe: () => Promise.reject(new TypeError("Failed to fetch")) });

    expect(await screen.findByText("No connection to the application.")).toBeInTheDocument();
  });

  it.each([502, 503, 504])("shows `ServiceUnavailablePage`, not the session expired screen, for HTTP %i", async (status) => {
    renderProvider({ onGetMe: () => Promise.reject(httpError(status)) });

    expect(await screen.findByText("The application cannot be reached right now.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Back" })).toBeInTheDocument();
    expect(screen.queryByText("Session expired")).not.toBeInTheDocument();
  });

  it.each<[string, () => unknown]>([
    ["HTTP 502", () => httpError(502)],
    ["HTTP 503", () => httpError(503)],
    ["HTTP 504", () => httpError(504)],
    ["a network error", () => new AxiosError("network down")],
  ])("keeps the application when a refetch of the loaded session fails with %s", async (_name, failure) => {
    const onGetMe = vi
      .fn<() => Promise<MeResponse>>()
      .mockResolvedValueOnce(meResponse)
      .mockImplementation(() => Promise.reject(failure()));
    const queryClient = renderProvider({ onGetMe });
    expect(await screen.findByText("Application")).toBeInTheDocument();

    await act(() => queryClient.refetchQueries({ queryKey: getMeQueryKey }));

    expect(onGetMe).toHaveBeenCalledTimes(2);
    expect(screen.getByText("Application")).toBeInTheDocument();
  });

  it("keeps `ServiceUnavailablePage` on screen while /me is fetched again during an outage", async () => {
    const onGetMe = vi
      .fn<() => Promise<MeResponse>>()
      .mockRejectedValueOnce(httpError(503))
      .mockImplementation(() => new Promise(() => undefined));
    const queryClient = renderProvider({ onGetMe });
    expect(await screen.findByText("The application cannot be reached right now.")).toBeInTheDocument();

    act(() => {
      void queryClient.refetchQueries({ queryKey: getMeQueryKey });
    });

    await waitFor(() => expect(queryClient.getQueryState(getMeQueryKey)?.status).toBe("pending"));
    // React Query hands the pending state to React in a timeout; this one renders it.
    await act(() => new Promise((resolve) => setTimeout(resolve, 0)));

    expect(onGetMe).toHaveBeenCalledTimes(2);
    expect(screen.getByText("The application cannot be reached right now.")).toBeInTheDocument();
    expect(screen.queryByText("Loading user data ...")).not.toBeInTheDocument();
  });

  it("renders the application once /me answers after an outage, on the retry button", async () => {
    const onGetMe = vi.fn<() => Promise<MeResponse>>().mockRejectedValueOnce(httpError(503)).mockResolvedValue(meResponse);
    renderProvider({ onGetMe });

    await userEvent.click(await screen.findByRole("button", { name: "Try again" }));

    expect(await screen.findByText("Application")).toBeInTheDocument();
  });

  it("renders the application once /me answers after an outage, on the retry interval", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      const onGetMe = vi.fn<() => Promise<MeResponse>>().mockRejectedValueOnce(httpError(503)).mockResolvedValue(meResponse);
      renderProvider({ onGetMe });
      expect(await screen.findByText("The application cannot be reached right now.")).toBeInTheDocument();

      await act(() => vi.advanceTimersByTimeAsync(5_000));

      expect(await screen.findByText("Application")).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it("hands a failure without a status to the error boundary when it is no network error", async () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    renderProvider({ onGetMe: () => Promise.reject(new Error("onGetMe threw")) });

    expect(await screen.findByText("Error boundary")).toBeInTheDocument();
    expect(screen.queryByText("The application cannot be reached right now.")).not.toBeInTheDocument();
  });

  it("hands a not-registered identity to the product's registration flow", async () => {
    const renderNotRegistered = vi.fn((): ReactElement => <div>Registration</div>);

    renderProvider({
      onGetMe: () => Promise.resolve<MeResponse>({ type: "bad/NotRegistered", selfSignupAllowed: true }),
      renderNotRegistered,
    });

    expect(await screen.findByText("Registration")).toBeInTheDocument();
    expect(renderNotRegistered).toHaveBeenCalledWith(expect.objectContaining({ selfSignupAllowed: true }));
  });

  it("signs out through the login gateway, which the app itself cannot do", async () => {
    // The app holds no token and no Keycloak instance: the only way out is the form POST the gateway answers with the logout.
    const submittedForms: HTMLFormElement[] = [];
    vi.spyOn(HTMLFormElement.prototype, "submit").mockImplementation(function (this: HTMLFormElement) {
      submittedForms.push(this);
    });

    renderProvider({
      onGetMe: () => Promise.resolve<MeResponse>({ type: "bad/NotRegistered", selfSignupAllowed: false }),
    });

    const signOut = await screen.findByRole("button", { name: "Sign out" });
    await userEvent.click(signOut);

    expect(submittedForms).toHaveLength(1);
    expect(submittedForms[0]?.method).toBe("post");
    expect(new URL(submittedForms[0]?.action ?? "").pathname).toBe("/auth/logout");
    // A browser drops the submit of a form outside the document.
    expect(submittedForms[0]?.isConnected).toBe(true);
    // The navigation is under way; a second click would start it again.
    expect(signOut).toBeDisabled();
  });

  it("falls back to the access-denied screen when the product has no registration flow", async () => {
    renderProvider({
      onGetMe: () => Promise.resolve<MeResponse>({ type: "bad/NotRegistered", selfSignupAllowed: false }),
      contactEmail: "info@neckar.it",
    });

    expect(await screen.findByText("Invitation required")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "info@neckar.it" })).toHaveAttribute("href", "mailto:info@neckar.it");
  });

  it("shows the sign-in-again screen for a same-origin 401 of a query, without a retry", async () => {
    const { httpClient, requests } = createUnauthorizedHttpClient();

    renderApplication(<BookPage url="/api/books/42" httpClient={httpClient} />);

    expect(await screen.findByText("Session expired")).toBeInTheDocument();
    expect(screen.queryByText("Error boundary")).not.toBeInTheDocument();
    expect(requests).toEqual(["/api/books/42"]);
  });

  it("shows the sign-in-again screen for a same-origin 401 of a mutation, without an error toast", async () => {
    const { httpClient } = createUnauthorizedHttpClient();
    const onMutationError = vi.fn();

    renderApplication(<SaveButton httpClient={httpClient} />, onMutationError);
    await userEvent.click(await screen.findByRole("button", { name: "Save" }));

    expect(await screen.findByText("Session expired")).toBeInTheDocument();
    expect(onMutationError).not.toHaveBeenCalled();
  });

  it("leaves a 401 of a foreign origin to the error boundary", async () => {
    const { httpClient, requests } = createUnauthorizedHttpClient();
    vi.spyOn(console, "error").mockImplementation(() => {});

    renderApplication(<BookPage url="https://other.example/api/x" httpClient={httpClient} />);

    expect(await screen.findByText("Error boundary")).toBeInTheDocument();
    expect(screen.queryByText("Session expired")).not.toBeInTheDocument();
    expect(requests).toHaveLength(3);
  });
});
