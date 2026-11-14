/**
 * Imports
 */

import { imageDigest, shortImageDigest, UnknownImageDigest } from "./imageDigest";

/**
 * The digest of the image the frontend is served from, injected at serve time (see `serve-time-injection`); every field falls
 * back to {@link UnknownImageDigest} when nothing was injected. The commit behind it is a lookup, see {@link imageDigestUrl}.
 */
export interface AppImageInfo {
  /** Digest of the image, `sha256:` plus 64 hex characters. */
  imageDigest: string;
  /** First 12 hex characters behind `sha256:` — for display, see {@link shortImageDigest}. */
  imageDigestShort: string;
}

/**
 * Cheap enough to call per render: one `querySelector` call.
 */
export function appImageInfo(): AppImageInfo {
  const digest = imageDigest();

  return {
    imageDigest: digest ?? UnknownImageDigest,
    imageDigestShort: digest === undefined ? UnknownImageDigest : shortImageDigest(digest),
  };
}

/**
 * Page on deployments.neckar.it naming the commit, push time and pipeline of the first push of
 * {@link imageDigest}.
 */
export function imageDigestUrl(imageDigest: string): string {
  return `https://deployments.neckar.it/digests/${imageDigest}`;
}
