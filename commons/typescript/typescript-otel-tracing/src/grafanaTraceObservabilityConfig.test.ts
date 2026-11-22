import { TraceIdPlaceholder } from "@neckar-hub/typescript-utils/observability";
import { afterEach, describe, expect, it, vi } from "vitest";
import { grafanaTraceObservabilityConfig } from "./grafanaTraceObservabilityConfig";

interface TracePanes {
  trace: {
    datasource: string;
    queries: { refId: string; datasource: { type: string; uid: string }; queryType: string; query: string }[];
    range: { from: string; to: string };
  };
}

function traceLinkTemplate(): URL {
  const template = grafanaTraceObservabilityConfig().traceLinkTemplate;
  if (template === undefined) {
    throw new Error("Expected a trace link template");
  }
  return new URL(template);
}

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("grafanaTraceObservabilityConfig", () => {
  it("links to the production monitoring host in a production build", () => {
    vi.stubEnv("PROD", true);

    expect(traceLinkTemplate().origin).toBe("https://monitoring.neckar.it");
  });

  it("links to the Explore page of the local monitoring host outside a production build", () => {
    const url = traceLinkTemplate();

    expect(url.origin).toBe("https://monitoring.localdev");
    expect(url.pathname).toBe("/explore");
    expect(url.searchParams.get("schemaVersion")).toBe("1");
    expect(url.searchParams.get("orgId")).toBe("1");
  });

  it("queries the Tempo datasource with the trace ID placeholder in the panes parameter", () => {
    const panes = JSON.parse(traceLinkTemplate().searchParams.get("panes") ?? "") as TracePanes;

    expect(panes.trace.datasource).toBe("tempo");
    expect(panes.trace.queries).toEqual([
      { refId: "A", datasource: { type: "tempo", uid: "tempo" }, queryType: "traceql", query: TraceIdPlaceholder },
    ]);
    expect(panes.trace.range).toEqual({ from: "now-1h", to: "now" });
  });

  it("keeps the placeholder literal in the template so buildTraceLinkUrl can substitute it", () => {
    const template = grafanaTraceObservabilityConfig().traceLinkTemplate ?? "";

    expect(template).toContain(TraceIdPlaceholder);
  });
});
