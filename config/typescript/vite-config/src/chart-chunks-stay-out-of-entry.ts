import type { Plugin } from "vite";

/**
 * Fails the build when an entry chunk reaches a chart chunk through static imports, which would load it on every page.
 * `isChartChunk` receives the chunk name.
 */
export function chartChunksStayOutOfEntry(isChartChunk: (chunkName: string) => boolean): Plugin {
  return {
    name: "chart-chunks-stay-out-of-entry",
    apply: "build",
    generateBundle(_options, bundle) {
      const chunksByFileName = new Map(
        Object.values(bundle).flatMap((output) => (output.type === "chunk" ? [[output.fileName, output] as const] : [])),
      );

      for (const entry of chunksByFileName.values()) {
        if (!entry.isEntry) continue;

        const reached = new Set<string>();
        const pending = [...entry.imports];
        for (let fileName = pending.pop(); fileName !== undefined; fileName = pending.pop()) {
          if (reached.has(fileName)) continue;
          reached.add(fileName);
          pending.push(...(chunksByFileName.get(fileName)?.imports ?? []));
        }

        const eagerChartChunks = [...reached].filter((fileName) => isChartChunk(chunksByFileName.get(fileName)?.name ?? ""));
        if (eagerChartChunks.length > 0) {
          this.error(
            `Entry chunk ${entry.fileName} imports the chart chunks ${eagerChartChunks.join(", ")} statically. ` +
              `Load the page or section that renders the chart through lazy() / import().`,
          );
        }
      }
    },
  };
}
