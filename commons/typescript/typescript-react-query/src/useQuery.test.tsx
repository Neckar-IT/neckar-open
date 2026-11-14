/**
 * Imports
 */

import { QueryClientProvider } from "@tanstack/react-query";
import { act, render, screen, waitFor } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createQueryClient } from "./createQueryClient";
import { QueryStatus, useQuery } from "./useQuery";

function unauthorized(): AxiosError {
  const config = { headers: new AxiosHeaders(), url: "/api/books/42" };
  return new AxiosError("unauthorized", AxiosError.ERR_BAD_REQUEST, config, undefined, {
    status: 401,
    statusText: "Unauthorized",
    data: {},
    headers: {},
    config,
  });
}

/** React Query hands an observer update to the component on the next timer tick. */
async function flushObserverUpdates(): Promise<void> {
  await act(() => new Promise((resolve) => setTimeout(resolve, 0)));
}

function BookStatus(props: { queryFn: () => Promise<string> }) {
  const book = useQuery({ queryKey: ["book"], queryFn: props.queryFn, retry: false });
  return <p data-testid="book">{book.status === QueryStatus.Pending ? "pending" : `${book.status} ${book.response}`}</p>;
}

/**
 * Test Component
 */

const queryClient = createQueryClient();

function TestComponent() {
  return (
    <QueryClientProvider client={queryClient}>
      <QueryComponent />
    </QueryClientProvider>
  );
}

function QueryComponent() {
  const queryResult = useQuery({
    queryKey: ["test"],
    queryFn: () => ({ type: "good", data: "some-data" }),
  });

  switch (queryResult.status) {
    case QueryStatus.Pending:
      return <div>Pending ...</div>;
    case QueryStatus.Fetching:
    case QueryStatus.Success:
      return (
        <div data-testid="resolved">
          <p data-testid="status">{queryResult.status}</p>
          <p data-testid="response.type">{queryResult.response.type}</p>
          <p data-testid="response.data">{queryResult.response.data}</p>
        </div>
      );
    default:
      throw new Error(`Unexpected value in Switch Statement: queryResult.status`);
  }
}

/**
 * Tests
 */

describe("useQuery", () => {
  // The cached answer of one test would otherwise skip the pending state of the next.
  afterEach(() => {
    queryClient.clear();
  });

  it("should return the correct status", async () => {
    render(<TestComponent />);

    expect(screen.getByText("Pending ...")).toBeInTheDocument();

    await screen.findByTestId("resolved");

    expect(screen.getByTestId("status").textContent).toBe("success");
  });

  it("should return the data as response", async () => {
    render(<TestComponent />);
    await screen.findByTestId("resolved");

    expect(screen.getByTestId("response.type").textContent).toBe("good");
    expect(screen.getByTestId("response.data").textContent).toBe("some-data");
  });
});

describe("useQuery under unauthorizedEndsSession", () => {
  it("stays pending after a same-origin 401 of the first fetch", async () => {
    const client = createQueryClient({ unauthorizedEndsSession: true });
    const queryFn = vi.fn(() => Promise.reject(unauthorized()));

    render(
      <QueryClientProvider client={client}>
        <BookStatus queryFn={queryFn} />
      </QueryClientProvider>,
    );

    await waitFor(() => expect(client.getQueryState(["book"])?.status).toBe("error"));
    await flushObserverUpdates();

    expect(screen.getByTestId("book").textContent).toBe("pending");
  });

  it("keeps the data after a same-origin 401 of a refetch", async () => {
    const client = createQueryClient({ unauthorizedEndsSession: true });
    const queryFn = vi.fn<() => Promise<string>>().mockResolvedValueOnce("book 42").mockRejectedValue(unauthorized());

    render(
      <QueryClientProvider client={client}>
        <BookStatus queryFn={queryFn} />
      </QueryClientProvider>,
    );
    await screen.findByText("success book 42");

    await client.refetchQueries({ queryKey: ["book"] });
    await flushObserverUpdates();

    expect(client.getQueryState(["book"])?.status).toBe("error");
    expect(screen.getByTestId("book").textContent).toBe("success book 42");
  });
});

describe("useQuery with an unavailable service", () => {
  it("keeps the data after a refetch failing with 503", async () => {
    const client = createQueryClient();
    const config = { headers: new AxiosHeaders(), url: "/api/books/42" };
    const serviceUnavailable = new AxiosError("unavailable", AxiosError.ERR_BAD_RESPONSE, config, undefined, {
      status: 503,
      statusText: "Service Unavailable",
      data: {},
      headers: {},
      config,
    });
    const queryFn = vi.fn<() => Promise<string>>().mockResolvedValueOnce("book 42").mockRejectedValue(serviceUnavailable);

    render(
      <QueryClientProvider client={client}>
        <BookStatus queryFn={queryFn} />
      </QueryClientProvider>,
    );
    await screen.findByText("success book 42");

    await client.refetchQueries({ queryKey: ["book"] });
    await flushObserverUpdates();

    expect(client.getQueryState(["book"])?.status).toBe("error");
    expect(screen.getByTestId("book").textContent).toBe("success book 42");
  });
});
