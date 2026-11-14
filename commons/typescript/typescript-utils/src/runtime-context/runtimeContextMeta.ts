/**
 * The `<meta>` tag names the runtime context travels under.
 *
 * The `runtime-context.` prefix tells them apart from the deploy-metadata tags next to them in the
 * head without looking anything up: these three come from the container environment and differ per
 * deployment, while the `imageDigest` tag names the image and is the same wherever it runs.
 */
export const RuntimeContextMetaNames = {
  executionEnvironment: "runtime-context.executionEnvironment",
  deploymentStage: "runtime-context.deploymentStage",
  serviceHost: "runtime-context.serviceHost",
} as const;
