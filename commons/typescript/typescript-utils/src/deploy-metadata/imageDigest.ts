/**
 * Imports
 */

import { readInjectedMeta } from "../serve-time-injection";
import { DeployMetadataMetaNames } from "./deployMetadataMeta";

/** `APP_IMAGE_DIGEST` of a deploy without pinned digest; mirrors `VersionInformation.UnpinnedImageDigest`. */
const UnpinnedImageDigest = "unpinned";

/** The value when nothing was injected; mirrors `VersionInformation.UnknownValue`. */
export const UnknownImageDigest = "unknown";

/** Mirrors `ImageDigestText.Algorithm`; the short form starts behind it. */
const DigestAlgorithm = "sha256:";

/** Mirrors `VersionInformation.ShortHashLength` and `printf '%.12s'` in `40-inject-serve-time-values.sh`. */
const ShortDigestLength = 12;

/**
 * A well-formed image digest: {@link DigestAlgorithm} plus 64 lowercase hex characters.
 */
const ImageDigestPattern = new RegExp(`^${DigestAlgorithm}[a-f0-9]{64}$`);

/**
 * `undefined` when nothing was injected, the deploy pinned no digest or the value is malformed: a page cannot fail its visitor
 * over a diagnosis value.
 */
export function imageDigest(): string | undefined {
  const injected = readInjectedMeta(DeployMetadataMetaNames.imageDigest);
  if (injected === undefined || injected === UnpinnedImageDigest) {
    return undefined;
  }
  return ImageDigestPattern.test(injected) ? injected : undefined;
}

/**
 * The first {@link ShortDigestLength} hex characters behind {@link DigestAlgorithm} — for display.
 * {@link UnknownImageDigest} for anything that is not a well-formed digest.
 */
export function shortImageDigest(imageDigest: string): string {
  return ImageDigestPattern.test(imageDigest)
    ? imageDigest.slice(DigestAlgorithm.length, DigestAlgorithm.length + ShortDigestLength)
    : UnknownImageDigest;
}
