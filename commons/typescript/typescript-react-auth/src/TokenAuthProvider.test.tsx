/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import type { LoginResponseGoodWithType, MeResponseGoodWithType } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import { delay } from "es-toolkit";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { TokenAuthConfig } from "./TokenAuthConfig";
import { TokenAuthProvider } from "./TokenAuthProvider";

/**
 * Mocks
 */

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
    tenant: null,
    capabilities: [],
    roleLabel: null,
  },
};

const loginResponse: LoginResponseGoodWithType = {
  type: "good",
  data: {
    jwsTokens: {
      accessToken: "valid-access-token",
      refreshToken: "valid-refresh-token",
    },
    loginName: "test-user",
  },
};

beforeEach(() => {
  vi.clearAllMocks();
});

/**
 * Tests
 */

describe("TokenAuthProvider", () => {
  it("should show the loading indicator while the query is pending", async () => {
    const originalLocation = window.location;

    // Override window.location with a URL object (or mock host)
    Object.defineProperty(window, "location", {
      value: new URL("https://demo.com?token=valid-login-token"),
      writable: true,
      configurable: true,
    });

    const authConfig: TokenAuthConfig = {
      onGetMe: async () => {
        await delay(500);
        return meResponse;
      },
      login: {
        onLoginWithToken: () => Promise.resolve(loginResponse),
      },
    };

    const queryClient = createQueryClient();

    render(
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <TokenAuthProvider config={authConfig}>
            <div>Application</div>
          </TokenAuthProvider>
        </QueryClientProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Authenticating ...")).toBeInTheDocument();

    // The pending query settles inside the test, so no timer or request outlives it.
    expect(await screen.findByText("Application")).toBeInTheDocument();

    // Restore the original window.location so it doesn't affect other tests
    Object.defineProperty(window, "location", {
      value: originalLocation,
      writable: true,
      configurable: true,
    });
  });

  it("should login with the token from the url and render the app if the user query failed with 401", async () => {
    const originalLocation = window.location;

    // Override window.location with a URL object (or mock host)
    Object.defineProperty(window, "location", {
      value: new URL("https://demo.com?token=valid-login-token"),
      writable: true,
      configurable: true,
    });

    const headers = new AxiosHeaders();
    headers.set("Authorization", "Bearer invalid-refresh-token");

    const error = new AxiosError("Unauthorized", AxiosError.ERR_BAD_REQUEST, undefined, null, {
      status: 401,
      statusText: "Unauthorized",
      headers,
      config: { method: "GET", url: "/me", headers },
      data: null,
    });

    const onGetMe = vi
      .fn()
      .mockImplementationOnce(() => Promise.reject(error))
      .mockImplementationOnce(() => Promise.resolve(meResponse));

    const onLoginWithToken = vi.fn().mockImplementation(() => Promise.resolve(loginResponse));

    const authConfig: TokenAuthConfig = {
      onGetMe,
      login: {
        onLoginWithToken,
      },
    };

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <TokenAuthProvider config={authConfig}>
            <div>Application</div>
          </TokenAuthProvider>
        </QueryClientProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Application")).toBeInTheDocument();
    expect(onLoginWithToken).toHaveBeenCalledTimes(1);

    // Restore the original window.location so it doesn't affect other tests
    Object.defineProperty(window, "location", {
      value: originalLocation,
      writable: true,
      configurable: true,
    });
  });

  it("should show an error message if the login failed because of a invalid token", async () => {
    const originalLocation = window.location;

    // Override window.location with a URL object (or mock host)
    Object.defineProperty(window, "location", {
      value: new URL("https://demo.com?token=valid-login-token"),
      writable: true,
      configurable: true,
    });

    const headers = new AxiosHeaders();
    headers.set("Authorization", "Bearer invalid-refresh-token");

    const error = new AxiosError("Unauthorized", AxiosError.ERR_BAD_REQUEST, undefined, null, {
      status: 401,
      statusText: "Unauthorized",
      headers,
      config: { method: "GET", url: "/me", headers },
      data: null,
    });

    const onGetMe = vi
      .fn()
      .mockImplementationOnce(() => Promise.reject(error))
      .mockImplementationOnce(() => Promise.resolve(meResponse));

    const onLoginWithToken = vi.fn().mockImplementation(() => Promise.resolve({ type: "bad/InvalidCredentials" as const }));

    const authConfig: TokenAuthConfig = {
      onGetMe,
      login: {
        onLoginWithToken,
      },
    };

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <TokenAuthProvider config={authConfig}>
            <div>Application</div>
          </TokenAuthProvider>
        </QueryClientProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Invalid Login Token.")).toBeInTheDocument();
    expect(onLoginWithToken).toHaveBeenCalledTimes(1);

    // Restore the original window.location so it doesn't affect other tests
    Object.defineProperty(window, "location", {
      value: originalLocation,
      writable: true,
      configurable: true,
    });
  });
});
