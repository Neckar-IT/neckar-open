import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { join } from "node:path";
import type { Plugin } from "vite";

/**
 * The `${…}` placeholders stay raw for /docker-entrypoint.d/40-inject-serve-time-values.sh of the showcase container;
 * `serveTimeInjection()` fills them in the preview frame of `storybook dev`. The manager page names the image only.
 */
const ImageDigestHead = `<meta name="imageDigest" content="\${APP_IMAGE_DIGEST}" />`;

/** The stories read the runtime context in the preview frame; components gate developer-only UI on the deployment stage. */
const PreviewServeTimeHead = `${ImageDigestHead}
  <meta name="runtime-context.executionEnvironment" content="\${EXECUTION_ENVIRONMENT}" />
  <meta name="runtime-context.deploymentStage" content="\${DEPLOYMENT_STAGE}" />
  <meta name="runtime-context.serviceHost" content="\${SERVICE_HOST}" />`;

/** `managerHead` of a `.storybook/main.ts`: appends the image digest tag. */
export function storybookManagerHead(head: string | undefined): string {
  return `${head ?? ""}\n  ${ImageDigestHead}`;
}

/** `previewHead` of a `.storybook/main.ts`: appends the image digest and the runtime context tags. */
export function storybookPreviewHead(head: string | undefined): string {
  return `${head ?? ""}\n  ${PreviewServeTimeHead}`;
}

const MswWorkerFileName = "mockServiceWorker.js";

/** Serves and emits the worker of the package's own `msw` at the site root, where `msw-storybook-addon` registers it. */
export function mswServiceWorker(): Plugin {
  let root = process.cwd();
  const worker = (): string => readFileSync(createRequire(join(root, "package.json")).resolve(`msw/${MswWorkerFileName}`), "utf8");

  return {
    name: "neckar-msw-service-worker",
    configResolved(config) {
      // Storybook sets the root to the package directory, the parent of `.storybook/`.
      root = config.root;
    },
    configureServer(server) {
      server.middlewares.use(`/${MswWorkerFileName}`, (_request, response) => {
        response.setHeader("Content-Type", "text/javascript");
        response.end(worker());
      });
    },
    generateBundle() {
      this.emitFile({ type: "asset", fileName: MswWorkerFileName, source: worker() });
    },
  };
}
