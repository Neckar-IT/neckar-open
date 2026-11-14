/**
 * Imports
 */

import { createQueryClient, QueryClientProvider, useQuery } from "@neckar-hub/typescript-react-query";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { Provider } from "jotai";
import { createMemoryRouter, RouterProvider } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { makeAxiosError } from "./axiosErrorFixture";
import { RouterErrorElement } from "./RouterErrorElement";

/**
 * Tests
 */

describe("RouterErrorElement", () => {
  it("fetches the failed query again on the retry button of `ServiceUnavailablePage`", async () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    const queryFn = vi
      .fn<() => Promise<string>>()
      .mockRejectedValueOnce(makeAxiosError({ status: 503 }))
      .mockResolvedValue("Book 42");
    function BookPage() {
      const book = useQuery({ queryKey: ["book"], queryFn, retry: false });
      return <div>{book.response}</div>;
    }
    const router = createMemoryRouter([{ path: "/", element: <BookPage />, errorElement: <RouterErrorElement /> }]);

    render(
      <Provider>
        <QueryClientProvider client={createQueryClient()}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </Provider>,
    );
    await userEvent.setup().click(await screen.findByRole("button", { name: "Try again" }));

    expect(await screen.findByText("Book 42")).toBeInTheDocument();
    expect(queryFn).toHaveBeenCalledTimes(2);
  });
});
