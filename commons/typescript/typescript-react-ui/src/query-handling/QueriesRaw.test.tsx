/**
 * Imports
 */

import { QueryStatus, createQueryClient, QueryClientProvider, useQueryForStaticData } from "@neckar-hub/typescript-react-query";
import { render, screen } from "@test/test-utils";
import { delay } from "es-toolkit";
import { describe, expect, it } from "vitest";
import { QueriesRaw } from "./QueriesRaw";

/**
 * Test Component
 */

function TestComponent({ queryFn }: { queryFn: () => Promise<{ type: "good"; data: string }> }) {
  const queryResult = useQueryForStaticData({
    queryKey: ["test-query"],
    queryFn,
  });

  return (
    <QueriesRaw queries={[queryResult]}>
      {([result]) => {
        switch (result.status) {
          case QueryStatus.Pending:
            return <div data-testid="pending-state">Loading custom</div>;
          case QueryStatus.Fetching:
          case QueryStatus.Success:
            return <div data-testid="query-data">{result.response.data}</div>;
          default:
            throw new Error(`Unexpected value in Switch Statement: result.status`);
        }
      }}
    </QueriesRaw>
  );
}

/**
 * Tests
 */

describe("QueriesRaw", () => {
  it("should call children with the query result when successful", async () => {
    const queryClient = createQueryClient();

    const mockQueryFn = () => Promise.resolve({ type: "good" as const, data: "some-data" });

    render(
      <QueryClientProvider client={queryClient}>
        <TestComponent queryFn={mockQueryFn} />
      </QueryClientProvider>,
    );

    expect(await screen.findByTestId("query-data")).toHaveTextContent("some-data");
  });

  it("should call children with PENDING status while loading", async () => {
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

    expect(screen.getByTestId("pending-state")).toHaveTextContent("Loading custom");
    expect(await screen.findByTestId("query-data")).toHaveTextContent("some-data");
  });
});
