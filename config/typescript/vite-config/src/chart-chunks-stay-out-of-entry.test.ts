import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { build } from "vite";
import { describe, expect, it } from "vitest";
import { chartChunksStayOutOfEntry } from "./chart-chunks-stay-out-of-entry.js";

/** Builds an entry with the given source; `chart.js` lands in a chunk called `meistercharts`. */
async function buildWith(entrySource: string): Promise<void> {
  const root = mkdtempSync(join(tmpdir(), "chart-chunks-"));
  mkdirSync(join(root, "src"));
  writeFileSync(join(root, "src", "main.js"), entrySource);
  writeFileSync(join(root, "src", "chart.js"), "export const chart = 'chart';\n");

  await build({
    root,
    configFile: false,
    logLevel: "silent",
    build: {
      write: false,
      rollupOptions: {
        input: join(root, "src", "main.js"),
        output: { manualChunks: (id) => (id.endsWith("chart.js") ? "meistercharts" : undefined) },
      },
    },
    plugins: [chartChunksStayOutOfEntry((chunkName) => chunkName === "meistercharts")],
  });
}

describe("chartChunksStayOutOfEntry", () => {
  it("fails the build when the entry imports a chart chunk statically", async () => {
    await expect(buildWith(`import { chart } from "./chart.js";\nconsole.log(chart);\n`)).rejects.toThrow(
      /Entry chunk .* imports the chart chunks .*meistercharts.* statically/,
    );
  });

  it("passes when the entry loads the chart chunk through import()", async () => {
    await expect(buildWith(`import("./chart.js").then((module) => console.log(module.chart));\n`)).resolves.toBeUndefined();
  });

  it("passes when no chunk is a chart chunk", async () => {
    const root = mkdtempSync(join(tmpdir(), "chart-chunks-"));
    mkdirSync(join(root, "src"));
    writeFileSync(join(root, "src", "main.js"), "console.log('plain');\n");

    await expect(
      build({
        root,
        configFile: false,
        logLevel: "silent",
        build: { write: false, rollupOptions: { input: join(root, "src", "main.js") } },
        plugins: [chartChunksStayOutOfEntry(() => false)],
      }),
    ).resolves.toBeDefined();
  });
});
