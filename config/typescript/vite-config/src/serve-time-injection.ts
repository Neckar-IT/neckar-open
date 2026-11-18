import type { Plugin } from "vite";
import { servePlaceholders } from "./serve-placeholders.js";

/**
 * The placeholder names the injection fills. Same list as the `envsubst` argument of
 * `internal/infrastructure/common/static-site/entrypoint.d/40-inject-serve-time-values.sh`, because
 * that list and the HTML heads are one interface: a name that drifts here is a tag that reads as
 * absent locally while the container fills it.
 */
const ServeTimePlaceholderNames = [
  "APP_IMAGE_DIGEST",
  "APP_IMAGE_DIGEST_SHORT",
  "EXECUTION_ENVIRONMENT",
  "DEPLOYMENT_STAGE",
  "SERVICE_HOST",
] as const;

type ServeTimePlaceholder = (typeof ServeTimePlaceholderNames)[number];

/**
 * What each placeholder is replaced with. `undefined` leaves the placeholder in the HTML, where the
 * readers of `@neckar-hub/typescript-utils/serve-time-injection` treat it as absent.
 */
export type ServeTimeValues = Record<ServeTimePlaceholder, string | undefined>;

/**
 * Copies `shortImageDigest` of typescript-utils, because this build-config package carries no runtime dependency.
 */
const ShortImageDigestLength = 12;

const ImageDigestAlgorithm = "sha256:";

const ImageDigestPattern = /^sha256:[a-f0-9]{64}$/;

/**
 * Values of `it.neckar.runtime.context.ExecutionEnvironment` and `DeploymentStage`, plus
 * `Hostname.localhost`: the same three the Gradle `run` tasks pass to the backend process via
 * `RuntimeContextEnv`, so frontend and backend of one local instance report the same environment.
 *
 * Spelled out rather than imported from the generated enums, which would put a Gradle-generated
 * OpenAPI artifact into the dependency chain of every frontend's Vite config.
 */
const DefaultExecutionEnvironment = "LocalDev";
const DefaultDeploymentStage = "Development";
const DefaultServiceHost = "localhost";

/**
 * Fills the serve-time placeholders while the Vite dev server serves the HTML — the local
 * counterpart of the nginx entrypoint, which substitutes the same names via `envsubst` at container
 * start. Same placeholders, same result in the head, different source.
 *
 * Without it the injection channel is only ever exercised in a deployment, where a break in it stays
 * invisible for as long as nobody reads a diagnosis header.
 *
 * Serve mode only, and that is the load-bearing part: `vite build` has to write the placeholders
 * into `dist/index.html` untouched, because a substituted value makes the build artifact differ per
 * environment and costs every frontend package its up-to-date state.
 */
export function serveTimeInjection(): Plugin {
  // After Storybook's hook, which appends the `previewHead` block carrying the placeholders of the
  // packages whose HTML shell ships with Storybook.
  return { ...servePlaceholders(resolveServeTimeValues, "post"), name: "neckar-serve-time-injection" };
}

/**
 * The environment variables carry the names of `RuntimeContextEnv`, so testing a frontend against
 * another stage takes the same variable that already configures the backend beside it. A local dev
 * server runs no image, so the digest stays a placeholder unless the environment sets a well-formed one.
 */
export function resolveServeTimeValues(): ServeTimeValues {
  const injectedImageDigest = process.env.APP_IMAGE_DIGEST;
  const imageDigest = injectedImageDigest !== undefined && ImageDigestPattern.test(injectedImageDigest) ? injectedImageDigest : undefined;

  return {
    APP_IMAGE_DIGEST: imageDigest,
    APP_IMAGE_DIGEST_SHORT: imageDigest?.slice(ImageDigestAlgorithm.length, ImageDigestAlgorithm.length + ShortImageDigestLength),
    EXECUTION_ENVIRONMENT: process.env.EXECUTION_ENVIRONMENT ?? DefaultExecutionEnvironment,
    DEPLOYMENT_STAGE: process.env.DEPLOYMENT_STAGE ?? DefaultDeploymentStage,
    SERVICE_HOST: process.env.SERVICE_HOST ?? DefaultServiceHost,
  };
}
