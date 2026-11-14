/**
 * Imports
 */

import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ImpersonationHeader, ImpersonationStorageKey } from "./impersonationStorage";
import { registerImpersonationAxiosInterceptor } from "./registerImpersonationAxiosInterceptor";

/**
 * An instance whose adapter answers with whether the request carried the header, or fails with
 * [failureBody] under HTTP 403.
 */
function createInstance(failureBody?: unknown): AxiosInstance {
  const instance = axios.create({
    adapter: (config: InternalAxiosRequestConfig) => {
      if (failureBody !== undefined) {
        const response = { data: failureBody, status: 403, statusText: "Forbidden", headers: {}, config };
        return Promise.reject(new AxiosError("rejected", AxiosError.ERR_BAD_REQUEST, config, undefined, response));
      }
      return Promise.resolve({
        data: config.headers.get(ImpersonationHeader) ?? "no header",
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      });
    },
  });
  registerImpersonationAxiosInterceptor(instance);
  return instance;
}

/**
 * Tests
 */

describe("registerImpersonationAxiosInterceptor", () => {
  const originalLocation = window.location;
  const assign = vi.fn();

  beforeEach(() => {
    window.sessionStorage.clear();
    assign.mockReset();
    Object.defineProperty(window, "location", { value: { assign }, writable: true, configurable: true });
  });

  afterEach(() => {
    Object.defineProperty(window, "location", { value: originalLocation, writable: true, configurable: true });
  });

  it("sends the stored user id in the header", async () => {
    window.sessionStorage.setItem(ImpersonationStorageKey, "user-1");

    const response = await createInstance().get("/books");

    expect(response.data).toBe("user-1");
  });

  it("sends no header while the tab acts as the signed-in user", async () => {
    const response = await createInstance().get("/books");

    expect(response.data).toBe("no header");
  });

  it("ends the impersonation when the backend rejects it", async () => {
    window.sessionStorage.setItem(ImpersonationStorageKey, "user-1");

    await expect(createInstance({ errorCode: "IMPERSONATION-1" }).get("/books")).rejects.toBeInstanceOf(AxiosError);

    expect(window.sessionStorage.getItem(ImpersonationStorageKey)).toBeNull();
    expect(assign).toHaveBeenCalledWith("/");
  });

  it("reloads nothing when a tab without impersonation receives a rejection", async () => {
    await expect(createInstance({ errorCode: "IMPERSONATION-1" }).get("/books")).rejects.toBeInstanceOf(AxiosError);

    expect(assign).not.toHaveBeenCalled();
  });

  it("keeps the impersonation on any other error", async () => {
    window.sessionStorage.setItem(ImpersonationStorageKey, "user-1");

    await expect(createInstance({ errorCode: "AUTHZ-1" }).get("/books")).rejects.toBeInstanceOf(AxiosError);

    expect(window.sessionStorage.getItem(ImpersonationStorageKey)).toBe("user-1");
    expect(assign).not.toHaveBeenCalled();
  });
});
