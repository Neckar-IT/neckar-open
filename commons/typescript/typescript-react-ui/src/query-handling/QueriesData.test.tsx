/**
 * Imports
 */

import { createQueryClient, QueryClientProvider, useQueryForStaticData } from "@neckar-hub/typescript-react-query";
import { render, screen } from "@test/test-utils";
import { delay } from "es-toolkit";
import { describe, expect, it } from "vitest";
import { QueriesData } from "./QueriesData";

/**
 * Test Component
 */

function TestComponent({ queryFn }: { queryFn: () => Promise<{ type: "good"; data: string } | { type: `bad/${string}` }> }) {
  const queryResult = useQueryForStaticData({
    queryKey: ["test-query"],
    queryFn,
  });

  return <QueriesData queries={[queryResult]}>{([data]) => <div data-testid="query-data">{data}</div>}</QueriesData>;
}

/**
 * Tests
 */

describe("QueriesData", () => {
  it("should render the data when the query is successful", async () => {
    const queryClient = createQueryClient();

    const mockQueryFn = () => Promise.resolve({ type: "good" as const, data: "some-data" });

    render(
      <QueryClientProvider client={queryClient}>
        <TestComponent queryFn={mockQueryFn} />
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId("query-data")).toHaveTextContent("some-data");
  });

  it("should render a loading spinner while the query is loading", async () => {
    const queryClient = createQueryClient();

    const mockQueryFn = async () => {
      await delay(50);
      return { type: "good" as const, data: "some-data" };
    };

    render(
      <QueryClientProvider client={queryClient}>
        <TestComponent queryFn={mockQueryFn} />
      </QueryClientProvider>,
    );

    expect(screen.getByText("Loading ...")).toBeInTheDocument();
    expect(await screen.findByTestId("query-data")).toHaveTextContent("some-data");
  });

  it("should render the fallback for failure/NotFound", async () => {
    const queryClient = createQueryClient();

    const mockQueryFn = () => Promise.resolve({ type: "bad/NotFound" as const });

    render(
      <QueryClientProvider client={queryClient}>
        <TestComponent queryFn={mockQueryFn} />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Not Found!")).toBeInTheDocument();
  });

  /**
   * An entity-specific not-found — `bad/InverterConfigurationNotFound` rather than the generic
   * `bad/NotFound` — names which entity is missing and is what every entity with its own response
   * union answers with. It is still a not-found and must render the fallback, not be rethrown into
   * an ErrorBoundary.
   */
  it("should render the fallback for an entity-specific NotFound", async () => {
    const queryClient = createQueryClient();

    const mockQueryFn = () => Promise.resolve({ type: "bad/InverterConfigurationNotFound" as const });

    render(
      <QueryClientProvider client={queryClient}>
        <TestComponent queryFn={mockQueryFn} />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Not Found!")).toBeInTheDocument();
  });
});
