import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { mswServiceWorker, storybookManagerHead, storybookPreviewHead } from "./storybook.js";

describe("storybookManagerHead and storybookPreviewHead", () => {
  it("append the image digest placeholder to the head Storybook hands over, also to none", () => {
    expect(storybookManagerHead("<title>x</title>")).toBe('<title>x</title>\n  <meta name="imageDigest" content="${APP_IMAGE_DIGEST}" />');
    expect(storybookManagerHead(undefined)).toBe('\n  <meta name="imageDigest" content="${APP_IMAGE_DIGEST}" />');
  });

  it("append the image digest and the runtime context placeholders to the head of the preview frame", () => {
    expect(storybookPreviewHead("<title>x</title>")).toBe(`<title>x</title>
  <meta name="imageDigest" content="\${APP_IMAGE_DIGEST}" />
  <meta name="runtime-context.executionEnvironment" content="\${EXECUTION_ENVIRONMENT}" />
  <meta name="runtime-context.deploymentStage" content="\${DEPLOYMENT_STAGE}" />
  <meta name="runtime-context.serviceHost" content="\${SERVICE_HOST}" />`);
  });
});

type EmittedAsset = { type: string; fileName: string; source: string };

describe("mswServiceWorker", () => {
  let packageRoot: string | undefined;

  afterEach(() => {
    if (packageRoot !== undefined) {
      rmSync(packageRoot, { recursive: true, force: true });
    }
  });

  it("emits the worker of the msw installed in the Vite root", () => {
    packageRoot = mkdtempSync(join(tmpdir(), "msw-service-worker-"));
    const msw = join(packageRoot, "node_modules", "msw");
    mkdirSync(join(msw, "lib"), { recursive: true });
    writeFileSync(join(packageRoot, "package.json"), "{}");
    writeFileSync(join(msw, "package.json"), JSON.stringify({ name: "msw", exports: { "./mockServiceWorker.js": "./lib/mockServiceWorker.js" } }));
    writeFileSync(join(msw, "lib", "mockServiceWorker.js"), "// worker of the root");
    const plugin = mswServiceWorker();
    const emitted: EmittedAsset[] = [];

    (plugin.configResolved as (config: { root: string }) => void)({ root: packageRoot });
    (plugin.generateBundle as (this: { emitFile: (asset: EmittedAsset) => void }) => void).call({ emitFile: (asset) => emitted.push(asset) });

    expect(emitted).toEqual([{ type: "asset", fileName: "mockServiceWorker.js", source: "// worker of the root" }]);
  });
});
