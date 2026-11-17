/**
 * Imports
 */

import { afterEach, describe, expect, it } from "vitest";
import { displayedBuildInfo, readDeploymentInfo } from "./deploymentInfo";

/**
 * Tests
 */

describe("displayedBuildInfo", () => {
  const injected = { imageDigest: "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef" };

  afterEach(() => {
    window.testMode = undefined;
  });

  it("shows the injected build value", () => {
    expect(displayedBuildInfo(injected)).toEqual(injected);
  });

  it("shows a fixed build value under screenshot tests", () => {
    window.testMode = true;

    const displayed = displayedBuildInfo({ imageDigest: undefined });

    expect(displayed.imageDigest).toMatch(/^sha256:[0-9a-f]{64}$/);
    expect(displayedBuildInfo(injected)).toEqual(displayed);
  });
});

describe("readDeploymentInfo", () => {
  afterEach(() => {
    window.testMode = undefined;
  });

  it("reports the fixed build value under screenshot tests", () => {
    window.testMode = true;

    expect(readDeploymentInfo().build).toEqual(displayedBuildInfo({ imageDigest: undefined }));
  });
});
