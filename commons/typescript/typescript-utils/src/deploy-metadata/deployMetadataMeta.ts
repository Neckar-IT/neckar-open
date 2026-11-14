/**
 * The `<meta>` tag name the deploy metadata travels under.
 *
 * No prefix, where the runtime-context tags next to it in the head carry `runtime-context.`: the image
 * digest is the same wherever the image runs.
 */
export const DeployMetadataMetaNames = {
  imageDigest: "imageDigest",
} as const;
