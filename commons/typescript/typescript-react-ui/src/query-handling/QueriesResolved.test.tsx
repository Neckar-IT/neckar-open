/**
 * Imports
 */

import { createQueryClient, QueryClientProvider, useQueryForStaticData } from "@neckar-hub/typescript-react-query";
import { render, screen } from "@test/test-utils";
import { delay } from "es-toolkit";
import { describe, expect, it } from "vitest";
import { QueriesResolved } from "./QueriesResolved";

/**
 * Test Component
 */

function TestComponent({ queryFn }: { queryFn: () => Promise<{ type: "good"; data: string }> }) {
  const queryResult = useQueryForStaticData({
    queryKey: ["test-query"],
    queryFn,
  });

  return (
    <QueriesResolved queries={[queryResult]}>{([result]) => <div data-testid="query-data">{result.response.data}</div>}</QueriesResolved>
  );
}

/**
 * Tests
 */

describe("QueriesResolved", () => {
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
});
