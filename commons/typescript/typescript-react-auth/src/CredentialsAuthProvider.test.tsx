/**
 * Imports
 */

import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { createQueryClient, QueryClientProvider } from "@neckar-hub/typescript-react-query";
import { useAutoPageTitle } from "@neckar-hub/typescript-react-ui/layout";
import { clearTokenPair, setTokenPair } from "@neckar-hub/typescript-react-utils/custom-axios-instance";
import { JotaiProvider, jotaiStore } from "@neckar-hub/typescript-react-utils/jotai";
import { DeploymentStage, type LoginResponseGoodWithType, type MeResponseGoodWithType } from "@neckar-hub/typescript-utils/openapi-types";
import { isDemoDeployment, RuntimeContextMetaNames } from "@neckar-hub/typescript-utils/runtime-context";
import { injectMeta, removeInjectedMeta } from "@neckar-hub/typescript-utils/serve-time-injection";
import { render, screen } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import { delay } from "es-toolkit";
import { useRef } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { CredentialsAuthConfig } from "./CredentialsAuthConfig";
import { CredentialsAuthProvider } from "./CredentialsAuthProvider";

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
    tenantId: null,
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

/**
 * The 401 the `getMe` query answers with when the token is not accepted. The provider branches on
 * the status alone — every other detail only satisfies the constructor.
 */
function unauthorizedError(): AxiosError {
  const headers = new AxiosHeaders();
  headers.set("Authorization", "Bearer invalid-token");

  return new AxiosError("Unauthorized", AxiosError.ERR_BAD_REQUEST, undefined, null, {
    status: 401,
    statusText: "Unauthorized",
    headers,
    config: { method: "GET", url: "/me", headers },
    data: null,
  });
}

/**
 * An auto-login a product activates on the demo stage.
 */
function demoAutoLoginConfig(
  onGetMe: CredentialsAuthConfig["onGetMe"],
  onLogin: CredentialsAuthConfig["login"]["onLogin"],
): CredentialsAuthConfig {
  return {
    authScreenHeader: "Login Header",
    onGetMe,
    login: { onLogin },
    autoLogin: {
      enabled: isDemoDeployment(),
      loginName: "demo",
      password: "demo",
    },
  };
}

/**
 * Stands in for an authenticated application page that derives its own document title.
 */
function ApplicationPage(): React.ReactElement {
  const containerRef = useRef<HTMLDivElement>(null);
  useAutoPageTitle(containerRef, { appName: "Test App" });

  return (
    <div ref={containerRef}>
      <h1>Current Projects</h1>
    </div>
  );
}

let originalTitle: string;
let originalLocation: Location;

beforeEach(() => {
  vi.clearAllMocks();
  originalTitle = document.title;
  originalLocation = window.location;
});

// Restored here rather than at the end of a test so a failing assertion does not leak into the next one.
afterEach(() => {
  document.title = originalTitle;
  removeInjectedMeta(RuntimeContextMetaNames.deploymentStage);
  Object.defineProperty(window, "location", {
    value: originalLocation,
    writable: true,
    configurable: true,
  });
});

/**
 * Tests
 */

describe("CredentialsAuthProvider", () => {
  it("should show the loading indicator while the getMe- Query is pending", async () => {
    const authConfig: CredentialsAuthConfig = {
      onGetMe: async () => {
        await delay(1000);
        return meResponse;
      },
      login: {
        onLogin: () => Promise.resolve(loginResponse),
      },
    };

    const queryClient = createQueryClient();

    render(
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <CredentialsAuthProvider config={authConfig}>
            <div>Application</div>
          </CredentialsAuthProvider>
        </QueryClientProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Authenticating ...")).toBeInTheDocument();
  });

  it("should render the children, if the user query is successful", async () => {
    const config: CredentialsAuthConfig = {
      authScreenHeader: "Login Header",
      onGetMe: () => Promise.resolve(meResponse),
      login: {
        onLogin: () => Promise.resolve(loginResponse),
      },
    };

    const queryClient = createQueryClient();

    render(
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <CredentialsAuthProvider config={config}>
            <div>Application</div>
          </CredentialsAuthProvider>
        </QueryClientProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Application")).toBeInTheDocument();
  });

  it("should render the login screen, if the user query returns a 401 status code", async () => {
    const config: CredentialsAuthConfig = {
      authScreenHeader: "Login Header",
      onGetMe: () => Promise.reject(unauthorizedError()),
      login: {
        onLogin: () => Promise.resolve(loginResponse),
      },
    };

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <QueryClientProvider client={queryClient}>
          <CredentialsAuthProvider config={config}>
            <div>Application</div>
          </CredentialsAuthProvider>
        </QueryClientProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Login Header")).toBeInTheDocument();
  });

  it("should refetch the user when the refreshToken is set to null", async () => {
    const getMe = vi
      .fn()
      .mockImplementationOnce(() => Promise.resolve(meResponse))
      .mockImplementationOnce(() => Promise.reject(unauthorizedError()));

    setTokenPair({ accessToken: "valid-access-token", refreshToken: "valid-refresh-token" });

    const config: CredentialsAuthConfig = {
      authScreenHeader: "Login Header",
      onGetMe: getMe,
      login: {
        onLogin: () => Promise.resolve(loginResponse),
      },
    };

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <JotaiProvider store={jotaiStore}>
          <QueryClientProvider client={queryClient}>
            <CredentialsAuthProvider config={config}>
              <div>Application</div>
            </CredentialsAuthProvider>
          </QueryClientProvider>
        </JotaiProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Application")).toBeInTheDocument();

    clearTokenPair();

    expect(await screen.findByText("Login Header")).toBeInTheDocument();
    expect(getMe).toHaveBeenCalledTimes(2);
  });

  it("should set the login screen document title when the user logs out", async () => {
    document.title = "Test App";

    const getMe = vi
      .fn()
      .mockImplementationOnce(() => Promise.resolve(meResponse))
      .mockImplementationOnce(() => Promise.reject(unauthorizedError()));

    setTokenPair({ accessToken: "valid-access-token", refreshToken: "valid-refresh-token" });

    const config: CredentialsAuthConfig = {
      authScreenHeader: "Login Header",
      onGetMe: getMe,
      login: {
        onLogin: () => Promise.resolve(loginResponse),
      },
    };

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <JotaiProvider store={jotaiStore}>
          <QueryClientProvider client={queryClient}>
            <CredentialsAuthProvider config={config}>
              <ApplicationPage />
            </CredentialsAuthProvider>
          </QueryClientProvider>
        </JotaiProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Current Projects")).toBeInTheDocument();
    expect(document.title).toBe("Current Projects · Test App");

    clearTokenPair();

    expect(await screen.findByText("Login Header")).toBeInTheDocument();
    expect(document.title).toBe("Sign In · Test App");
  });

  it("should set the registration screen document title on /register", async () => {
    document.title = "Test App";

    Object.defineProperty(window, "location", {
      value: new URL("https://app.com/register"),
      writable: true,
      configurable: true,
    });

    const config: CredentialsAuthConfig = {
      authScreenHeader: "Login Header",
      onGetMe: () => Promise.resolve(meResponse),
      login: {
        onLogin: () => Promise.resolve(loginResponse),
      },
      userRegistration: {
        enabled: true,
        onRegister: () => Promise.reject(new Error("registration is not submitted in this test")),
      },
    };

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <JotaiProvider store={jotaiStore}>
          <QueryClientProvider client={queryClient}>
            <CredentialsAuthProvider config={config}>
              <ApplicationPage />
            </CredentialsAuthProvider>
          </QueryClientProvider>
        </JotaiProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Create Account")).toBeInTheDocument();
    expect(document.title).toBe("Create Account · Test App");
  });

  it("should automatically sign in the demo user on the demo stage, if the auto-login is enabled", async () => {
    injectMeta(RuntimeContextMetaNames.deploymentStage, DeploymentStage.Demo);

    const getMe = vi
      .fn()
      .mockImplementationOnce(() => Promise.reject(unauthorizedError()))
      .mockImplementationOnce(() => Promise.resolve(meResponse));

    const login = vi.fn().mockImplementation(() => Promise.resolve(loginResponse));

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <JotaiProvider store={jotaiStore}>
          <QueryClientProvider client={queryClient}>
            <CredentialsAuthProvider config={demoAutoLoginConfig(getMe, login)}>
              <div>Application</div>
            </CredentialsAuthProvider>
          </QueryClientProvider>
        </JotaiProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Application")).toBeInTheDocument();
    expect(login).toHaveBeenCalledTimes(1);
  });

  it("should not sign in the demo user on any other stage", async () => {
    injectMeta(RuntimeContextMetaNames.deploymentStage, DeploymentStage.Production);

    const getMe = vi.fn().mockImplementation(() => Promise.reject(unauthorizedError()));
    const login = vi.fn().mockImplementation(() => Promise.resolve(loginResponse));

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <JotaiProvider store={jotaiStore}>
          <QueryClientProvider client={queryClient}>
            <CredentialsAuthProvider config={demoAutoLoginConfig(getMe, login)}>
              <div>Application</div>
            </CredentialsAuthProvider>
          </QueryClientProvider>
        </JotaiProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Login Header")).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it("should sign in the demo user on the `auto-login` query parameter, even on a stage that does not activate it", async () => {
    injectMeta(RuntimeContextMetaNames.deploymentStage, DeploymentStage.Production);
    Object.defineProperty(window, "location", {
      value: new URL("https://app.com/?auto-login=true"),
      writable: true,
      configurable: true,
    });

    const getMe = vi
      .fn()
      .mockImplementationOnce(() => Promise.reject(unauthorizedError()))
      .mockImplementationOnce(() => Promise.resolve(meResponse));

    const login = vi.fn().mockImplementation(() => Promise.resolve(loginResponse));

    const queryClient = createQueryClient();
    queryClient.setDefaultOptions({ queries: { retry: false } });

    render(
      <I18nProvider>
        <JotaiProvider store={jotaiStore}>
          <QueryClientProvider client={queryClient}>
            <CredentialsAuthProvider config={demoAutoLoginConfig(getMe, login)}>
              <div>Application</div>
            </CredentialsAuthProvider>
          </QueryClientProvider>
        </JotaiProvider>
      </I18nProvider>,
    );

    expect(await screen.findByText("Application")).toBeInTheDocument();
    expect(login).toHaveBeenCalledTimes(1);
  });
});
