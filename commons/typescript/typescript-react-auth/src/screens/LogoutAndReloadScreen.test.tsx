/**
 * Imports
 */

import { I18nProvider, type TextKey } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { clearTokenPair, getAccessToken, getRefreshToken, setTokenPair } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { render, screen, waitFor } from "@testing-library/react";
import { Component, type PropsWithChildren, type ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { LogoutContextProvider } from "../context/LogoutContext";
import { LogoutAndReloadScreen } from "./LogoutAndReloadScreen";

/**
 * Mocks
 */

const replace = vi.fn();
let originalLocation: Location;

beforeEach(() => {
  vi.clearAllMocks();
  // The signed-in state every logout starts from.
  setTokenPair({ accessToken: "valid-access-token", refreshToken: "valid-refresh-token" });
  originalLocation = window.location;
  Object.defineProperty(window, "location", {
    value: { replace },
    writable: true,
    configurable: true,
  });
});

// Restored here rather than at the end of a test so a failing assertion does not leak into the next one.
afterEach(() => {
  clearTokenPair();
  Object.defineProperty(window, "location", {
    value: originalLocation,
    writable: true,
    configurable: true,
  });
});

/** Stands in for the application's GlobalErrorHandler and collects what the screen throws. */
class CollectingErrorBoundary extends Component<PropsWithChildren<{ onError: (error: unknown) => void }>, { caught: boolean }> {
  override state = { caught: false };

  static getDerivedStateFromError(): { caught: boolean } {
    return { caught: true };
  }

  override componentDidCatch(error: unknown): void {
    this.props.onError(error);
  }

  override render(): ReactNode {
    return this.state.caught ? <p>Error page</p> : this.props.children;
  }
}

function renderLogoutScreen(onLogout: () => Promise<void>, onError: (error: unknown) => void = () => {}, textKey?: TextKey): void {
  const queryClient = createQueryClient();

  render(
    <I18nProvider>
      <QueryClientProvider client={queryClient}>
        <CollectingErrorBoundary onError={onError}>
          <LogoutContextProvider onLogout={onLogout}>
            <LogoutAndReloadScreen textKey={textKey} />
          </LogoutContextProvider>
        </CollectingErrorBoundary>
      </QueryClientProvider>
    </I18nProvider>,
  );
}

/** A logout the test finishes itself, so the redirect it triggers happens inside the test. */
function createPendingLogout() {
  let finish: () => void = () => {};
  const onLogout = () =>
    new Promise<void>((resolve) => {
      finish = resolve;
    });
  return { onLogout, finish: () => finish() };
}

/**
 * Tests
 */

describe("LogoutAndReloadScreen", () => {
  it("should show the loading indicator while the logout is pending", async () => {
    const logout = createPendingLogout();
    renderLogoutScreen(logout.onLogout);

    expect(await screen.findByText("Signing out ...")).toBeInTheDocument();

    logout.finish();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });

  it("should not render the pending state of the mutation as text", async () => {
    const logout = createPendingLogout();
    renderLogoutScreen(logout.onLogout);

    // Awaited first, so the assertion below sees the rendered screen
    expect(await screen.findByText("Signing out ...")).toBeInTheDocument();
    expect(screen.queryByText(/pending/i)).not.toBeInTheDocument();

    logout.finish();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });

  it("should show the product's own text key instead of the default one", async () => {
    const logout = createPendingLogout();
    renderLogoutScreen(logout.onLogout, () => {}, "authProvider.signOut");

    expect(await screen.findByText("Sign out")).toBeInTheDocument();
    expect(screen.queryByText("Signing out ...")).not.toBeInTheDocument();

    logout.finish();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });

  it("should end the server session and then redirect to the start page", async () => {
    const onLogout = vi.fn().mockResolvedValue(undefined);

    renderLogoutScreen(onLogout);

    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/");
    });

    // No request config: the interceptor's access token authenticates the route
    expect(onLogout).toHaveBeenCalledTimes(1);
    expect(onLogout).toHaveBeenCalledWith();
  });

  it("should surface a server logout that failed instead of redirecting", async () => {
    const caught: unknown[] = [];
    renderLogoutScreen(
      () => Promise.reject(new Error("the server refused the logout")),
      (error) => caught.push(error),
    );

    // The failure reaches the error boundary - the application shows its error page
    await waitFor(() => {
      expect(caught).toHaveLength(1);
    });
    expect(replace).not.toHaveBeenCalled();

    // Signed out either way: the tokens are cleared before the error propagates
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });
});
