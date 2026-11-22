import { beforeEach, describe, expect, it, vi } from "vitest";

const calls: string[] = [];

vi.mock("@neckar-hub/typescript-utils/deploy-metadata", () => ({
  appImageInfo: () => ({ imageDigestShort: "sha256:abc123" }),
}));
vi.mock("./clientErrorReporter", () => ({
  installClientErrorReporter: vi.fn(() => calls.push("installClientErrorReporter")),
}));
vi.mock("./initializeOtel", () => ({
  initializeOtel: vi.fn(() => calls.push("initializeOtel")),
}));
vi.mock("./windowErrorLogger", () => ({
  installWindowErrorLogger: vi.fn(() => calls.push("installWindowErrorLogger")),
}));
vi.mock("./installConsoleLogger", () => ({
  installConsoleLogger: vi.fn(() => calls.push("installConsoleLogger")),
}));

const { bootstrapObservability } = await import("./bootstrapObservability");
const { installClientErrorReporter } = await import("./clientErrorReporter");
const { initializeOtel } = await import("./initializeOtel");

describe("bootstrapObservability", () => {
  beforeEach(() => {
    calls.length = 0;
    vi.clearAllMocks();
  });

  it("installs the error reporter, OpenTelemetry, the window error logger and the console logger in this order", () => {
    bootstrapObservability({ serviceName: "patterns" });

    expect(calls).toEqual(["installClientErrorReporter", "initializeOtel", "installWindowErrorLogger", "installConsoleLogger"]);
  });

  it("passes the service name and the short image digest as service version", () => {
    bootstrapObservability({ serviceName: "patterns" });

    expect(installClientErrorReporter).toHaveBeenCalledWith({ serviceVersion: "sha256:abc123" });
    expect(initializeOtel).toHaveBeenCalledWith({ serviceName: "patterns", serviceVersion: "sha256:abc123", exportHeaders: undefined });
  });

  it("passes the export headers to initializeOtel", () => {
    const exportHeaders = () => Promise.resolve({ Authorization: "Bearer token" });

    bootstrapObservability({ serviceName: "huddle-ui", exportHeaders });

    expect(initializeOtel).toHaveBeenCalledWith({ serviceName: "huddle-ui", serviceVersion: "sha256:abc123", exportHeaders });
  });
});
