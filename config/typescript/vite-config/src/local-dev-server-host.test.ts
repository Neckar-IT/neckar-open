import { afterEach, describe, expect, it, vi } from "vitest";
import { localDevServerHost } from "./local-dev-server-host.js";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("localDevServerHost", () => {
  it("returns the value of VITE_HOST", () => {
    vi.stubEnv("VITE_HOST", "192.0.2.7");

    expect(localDevServerHost()).toBe("192.0.2.7");
  });

  it("returns the loopback address without VITE_HOST", () => {
    vi.stubEnv("VITE_HOST", undefined);

    expect(localDevServerHost()).toBe("127.0.0.1");
  });

  it.each(["0.0.0.0", "::", "true"])("rejects VITE_HOST=%s, which listens on every interface", (viteHost) => {
    vi.stubEnv("VITE_HOST", viteHost);

    expect(() => localDevServerHost()).toThrow("listens on every interface");
  });

  it("returns the loopback address for an empty VITE_HOST", () => {
    vi.stubEnv("VITE_HOST", "");

    expect(localDevServerHost()).toBe("127.0.0.1");
  });
});
