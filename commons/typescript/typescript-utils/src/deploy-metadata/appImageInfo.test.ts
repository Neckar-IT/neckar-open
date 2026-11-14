/**
 * @vitest-environment jsdom
 *
 * The package tests in a node environment; this resolver reads `<meta>` tags, so this file needs a
 * DOM.
 */

/**
 * Imports
 */

import { afterEach, describe, expect, it } from "vitest";
import { injectMeta } from "../serve-time-injection/serveTimeInjectionTestSupport";
import { appImageInfo, imageDigestUrl } from "./appImageInfo";
import { DeployMetadataMetaNames } from "./deployMetadataMeta";
import { imageDigest, shortImageDigest, UnknownImageDigest } from "./imageDigest";

/**
 * Helpers
 */

const Digest = "sha256:ad2453b02334597670fc64befad42c10ec0870d092c58b8ae0ca6ef1e903d221";
const ShortDigest = "ad2453b02334";

afterEach(() => {
  document.head.replaceChildren();
});

/**
 * Tests
 */

describe("imageDigest", () => {
  it("reads the imageDigest meta tag", () => {
    injectMeta(DeployMetadataMetaNames.imageDigest, Digest);

    expect(imageDigest()).toBe(Digest);
  });

  it("reports nothing when the tag is absent", () => {
    expect(imageDigest()).toBeUndefined();
  });

  it("reports nothing for the digest of a branch deploy without pin", () => {
    injectMeta(DeployMetadataMetaNames.imageDigest, "unpinned");

    expect(imageDigest()).toBeUndefined();
  });

  it("reports nothing for a malformed digest", () => {
    injectMeta(DeployMetadataMetaNames.imageDigest, "sha256:not-hex");

    expect(imageDigest()).toBeUndefined();
  });
});

describe("shortImageDigest", () => {
  it("takes the hex characters behind the algorithm, as the injecting entrypoint prints them", () => {
    expect(shortImageDigest(Digest)).toBe(ShortDigest);
  });

  it("names a malformed value unknown", () => {
    expect(shortImageDigest("sha256:not-hex")).toBe(UnknownImageDigest);
  });
});

describe("imageDigestUrl", () => {
  it("links the digest page of deployments.neckar.it", () => {
    expect(imageDigestUrl(Digest)).toBe(`https://deployments.neckar.it/digests/${Digest}`);
  });
});

describe("appImageInfo", () => {
  it("reads the injected digest", () => {
    injectMeta(DeployMetadataMetaNames.imageDigest, Digest);

    expect(appImageInfo()).toEqual({ imageDigest: Digest, imageDigestShort: ShortDigest });
  });

  it("reports unknown when nothing is injected", () => {
    expect(appImageInfo()).toEqual({ imageDigest: UnknownImageDigest, imageDigestShort: UnknownImageDigest });
  });

  it("treats an unreplaced placeholder as absent — HTML served without the injecting entrypoint", () => {
    injectMeta(DeployMetadataMetaNames.imageDigest, "${APP_IMAGE_DIGEST}");

    expect(appImageInfo().imageDigest).toBe(UnknownImageDigest);
  });

  it("treats an empty value as absent — envsubst ran without the env var", () => {
    injectMeta(DeployMetadataMetaNames.imageDigest, "");

    expect(appImageInfo().imageDigest).toBe(UnknownImageDigest);
  });
});
