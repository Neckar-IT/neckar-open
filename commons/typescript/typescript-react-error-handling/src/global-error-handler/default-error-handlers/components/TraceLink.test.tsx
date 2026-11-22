/**
 * Imports
 */

import type { ObservabilityConfig } from "@neckar-hub/typescript-utils/observability";
import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { makeAxiosError } from "../../axiosErrorFixture";
import { ObservabilityProvider } from "../../ObservabilityContext";
import { TraceLink } from "./TraceLink";

/**
 * Helpers
 */

const TRACE_ID = "0af7651916cd43dd8448eb211c80319c";

function renderTraceLink(error: unknown, observability?: ObservabilityConfig) {
  return render(
    <ObservabilityProvider observability={observability}>
      <TraceLink error={error} />
    </ObservabilityProvider>,
  );
}

/**
 * Tests
 */

describe("TraceLink", () => {
  it("renders nothing when the error carries no trace ID", () => {
    const { container } = renderTraceLink(makeAxiosError({ status: 500 }));
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the trace ID as plain text when no link template is configured", () => {
    renderTraceLink(makeAxiosError({ status: 500, traceId: TRACE_ID }));
    expect(screen.getByText(TRACE_ID)).toBeInTheDocument();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });

  it("renders a deep link into the Tempo trace when a template is configured", () => {
    renderTraceLink(makeAxiosError({ status: 500, traceId: TRACE_ID }), {
      traceLinkTemplate: "https://grafana.example/explore?trace=__TRACE_ID__",
    });

    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", `https://grafana.example/explore?trace=${TRACE_ID}`);
    expect(link).toHaveAttribute("target", "_blank");
  });

  it("falls back to the app-supplied trace ID getter for a non-axios error", () => {
    renderTraceLink(new Error("plain"), { getCurrentTraceId: () => TRACE_ID });
    expect(screen.getByText(TRACE_ID)).toBeInTheDocument();
  });
});
