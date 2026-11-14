/**
 * Imports
 */

import { PlatformHeader } from "@neckar-hub/typescript-utils/platform-headers";
import { AxiosError, AxiosHeaders, type AxiosRequestConfig } from "axios";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createCustomAxiosInstance } from "./createCustomAxiosInstance";

/**
 * Mocked Modules
 */

const mocks = vi.hoisted(() => ({
  getAccessToken: vi.fn().mockReturnValue("valid-access-token"),
  getRefreshToken: vi.fn().mockReturnValue("valid-refresh-token"),
  setTokenPair: vi.fn(),
  clearTokenPair: vi.fn(),
  getClientId: vi.fn().mockReturnValue("some-client-id"),
}));

vi.mock("./jwtTokenPair.store", () => ({
  getAccessToken: mocks.getAccessToken,
  getRefreshToken: mocks.getRefreshToken,
  setTokenPair: mocks.setTokenPair,
  clearTokenPair: mocks.clearTokenPair,
}));

vi.mock("./clientId", () => ({
  getClientId: mocks.getClientId,
}));

/**
 * Mock Data
 */

const user = {
  loginName: "braunbeton",
  firstName: "Braun",
  lastName: "Beton",
  email: "braunbeton@neckar.it",
  avatarUrl: null,
};

const tokens = {
  accessToken: "valid-access-token",
  refreshToken: "valid-refresh-token",
};

/**
 * HTTP Client
 */

const baseUrl = "http://localhost:3000";

const authMeUrl = "/auth/me";
const refreshAccessTokenUrl = "/auth/refresh-access-token";

const axiosInstance = createCustomAxiosInstance({
  baseUrl,
  onRefreshTokenPair: async (options?: AxiosRequestConfig) => {
    if (options?.headers?.Authorization === "Bearer valid-refresh-token") {
      return Promise.resolve({ type: "good", data: tokens });
    } else {
      return Promise.resolve({ type: "bad/InvalidRefreshToken" });
    }
  },
  unauthenticatedRequestUrls: [refreshAccessTokenUrl],
  featureFlags: "some-feature-flags",
  gitHash: "test-git-hash",
});

const httpClient = async <T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<T> => {
  const response = await axiosInstance<T>({ ...config, ...options });
  return response.data;
};

const getApiAuthMe = () => httpClient<{ type: string; data: typeof user }>({ url: authMeUrl, method: "GET" });

/**
 * REST handlers
 */

const authMeHandler = http.get(`${baseUrl}${authMeUrl}`, ({ request }) => {
  if (request.headers.get("Authorization") === "Bearer valid-access-token") {
    return HttpResponse.json({ type: "good", data: user });
  } else {
    return new HttpResponse("Unauthorized", { status: 401 });
  }
});

const authRefreshHandler = http.post(`${baseUrl}${refreshAccessTokenUrl}`, ({ request }) => {
  if (request.headers.get("Authorization") === "Bearer valid-refresh-token") {
    return HttpResponse.json({ type: "sucess", data: tokens });
  } else {
    return new HttpResponse("Unauthorized", { status: 401 });
  }
});

const requestIdsHandler = http.get(`${baseUrl}/test/ids`, ({ request }) => {
  return HttpResponse.json({
    clientId: request.headers.get(PlatformHeader.ClientId),
    requestId: request.headers.get("X-Request-Id"),
  });
});

const featureFlagsHandler = http.get(`${baseUrl}/test/feature-flags`, ({ request }) => {
  return HttpResponse.json({
    featureFlags: request.headers.get(PlatformHeader.FeatureFlags),
  });
});

const protocolVersionHandler = http.get(`${baseUrl}/test/protocol-version`, ({ request }) => {
  return HttpResponse.json({
    protocolVersion: request.headers.get(PlatformHeader.ProtocolVersion),
  });
});

const acceptHeaderHandler = http.get(`${baseUrl}/test/accept-header`, ({ request }) => {
  return HttpResponse.json({
    accept: request.headers.get("Accept"),
  });
});

/**
 * Tests
 */

describe("httpClient", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("should add the access token from the auth store", async () => {
    const server = setupServer(authMeHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await getApiAuthMe();
    server.close();

    expect(data).toEqual({ type: "good", data: user });
  });

  it("should try to refresh the tokens if the access token is invalid", async () => {
    mocks.getAccessToken.mockReturnValueOnce("invalid-access-token");

    const server = setupServer(authMeHandler, authRefreshHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await getApiAuthMe();
    server.close();

    expect(data).toEqual({ type: "good", data: user });
    expect(mocks.setTokenPair).toHaveBeenCalledWith(tokens);
  });

  it("should logout if both tokens are invalid", async () => {
    mocks.getAccessToken.mockReturnValueOnce("invalid-access-token");
    mocks.getRefreshToken.mockReturnValueOnce("invalid-refresh-token");

    const server = setupServer(authMeHandler, authRefreshHandler);
    server.listen({ onUnhandledRequest: "error" });

    let errorWasThrown = false;
    try {
      await getApiAuthMe();
    } catch (_ignore) {
      errorWasThrown = true;
    }

    server.close();

    expect(errorWasThrown).toBe(true);
    expect(mocks.clearTokenPair).toHaveBeenCalled();
  });

  it("should keep the token pair when the refresh fails transiently (network error)", async () => {
    mocks.getAccessToken.mockReturnValueOnce("invalid-access-token");

    const networkErrorAxiosInstance = createCustomAxiosInstance({
      baseUrl,
      onRefreshTokenPair: async () => {
        throw new AxiosError("Network Error", AxiosError.ERR_NETWORK);
      },
      unauthenticatedRequestUrls: [refreshAccessTokenUrl],
      featureFlags: "some-feature-flags",
      gitHash: "test-git-hash",
    });

    const server = setupServer(authMeHandler);
    server.listen({ onUnhandledRequest: "error" });

    let errorWasThrown = false;
    try {
      await networkErrorAxiosInstance({ url: authMeUrl, method: "GET" });
    } catch (_ignore) {
      errorWasThrown = true;
    }

    server.close();

    expect(errorWasThrown).toBe(true);
    // A transient failure must NOT end the session — the next 401 retries the refresh
    expect(mocks.clearTokenPair).not.toHaveBeenCalled();
  });

  it("should keep the token pair when the refresh fails with a server error (503)", async () => {
    mocks.getAccessToken.mockReturnValueOnce("invalid-access-token");

    const serverErrorAxiosInstance = createCustomAxiosInstance({
      baseUrl,
      onRefreshTokenPair: async () => {
        throw new AxiosError("Service Unavailable", AxiosError.ERR_BAD_RESPONSE, undefined, null, {
          status: 503,
          statusText: "Service Unavailable",
          headers: {},
          config: { headers: new AxiosHeaders() },
          data: null,
        });
      },
      unauthenticatedRequestUrls: [refreshAccessTokenUrl],
      featureFlags: "some-feature-flags",
      gitHash: "test-git-hash",
    });

    const server = setupServer(authMeHandler);
    server.listen({ onUnhandledRequest: "error" });

    let errorWasThrown = false;
    try {
      await serverErrorAxiosInstance({ url: authMeUrl, method: "GET" });
    } catch (_ignore) {
      errorWasThrown = true;
    }

    server.close();

    expect(errorWasThrown).toBe(true);
    // A transient failure must NOT end the session — the next 401 retries the refresh
    expect(mocks.clearTokenPair).not.toHaveBeenCalled();
  });

  it("should add the client and request id to the header", async () => {
    const server = setupServer(requestIdsHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await httpClient<{ clientId: string; requestId: string }>({ url: "/test/ids", method: "GET" });
    server.close();

    expect(data.clientId).not.toBeNull();
    expect(data.clientId).toBe("some-client-id");

    expect(data.requestId).not.toBeNull();
    expect(data.requestId.length).toBe(10);
  });

  it("should add the feature flags to the header", async () => {
    const server = setupServer(featureFlagsHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await httpClient<{ featureFlags: string }>({ url: "/test/feature-flags", method: "GET" });
    server.close();

    expect(data.featureFlags).toBe("some-feature-flags");
  });

  it("should add the git hash as NeckarIT-Protocol-Version header", async () => {
    const server = setupServer(protocolVersionHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await httpClient<{ protocolVersion: string }>({ url: "/test/protocol-version", method: "GET" });
    server.close();

    expect(data.protocolVersion).toBe("test-git-hash");
  });

  it("should state no protocol version without a git hash", async () => {
    const server = setupServer(protocolVersionHandler);
    server.listen({ onUnhandledRequest: "error" });
    const instanceWithoutGitHash = createCustomAxiosInstance({
      baseUrl,
      onRefreshTokenPair: async () => Promise.resolve({ type: "bad/InvalidRefreshToken" }),
      unauthenticatedRequestUrls: [],
    });

    const response = await instanceWithoutGitHash<{ protocolVersion: string | null }>({ url: "/test/protocol-version", method: "GET" });
    server.close();

    expect(response.data.protocolVersion).toBeNull();
  });

  it("should set Accept header to application/json by default", async () => {
    const server = setupServer(acceptHeaderHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await httpClient<{ accept: string }>({ url: "/test/accept-header", method: "GET" });
    server.close();

    expect(data.accept).toBe("application/json");
  });

  it("should not override an explicitly set Accept header", async () => {
    const server = setupServer(acceptHeaderHandler);
    server.listen({ onUnhandledRequest: "error" });

    const data = await httpClient<{ accept: string }>({
      url: "/test/accept-header",
      method: "GET",
      headers: { Accept: "text/plain" },
    });
    server.close();

    expect(data.accept).toBe("text/plain");
  });
});
