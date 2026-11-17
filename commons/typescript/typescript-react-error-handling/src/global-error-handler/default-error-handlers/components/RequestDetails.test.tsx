/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { makeAxiosError } from "../../axiosErrorFixture";
import { RequestDetails } from "./RequestDetails";

/**
 * Tests
 */

describe("RequestDetails", () => {
  it("renders nothing for a non-axios error", () => {
    const { container } = render(<RequestDetails error={new Error("plain")} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows method, URL and status for an axios error", () => {
    render(
      <RequestDetails
        error={makeAxiosError({ status: 500, statusText: "Internal Server Error", method: "post", url: "/api/patterns/books" })}
        initiallyOpen={true}
      />,
    );
    expect(screen.getByText(/POST \/api\/patterns\/books/)).toBeInTheDocument();
    expect(screen.getByText(/500 Internal Server Error/)).toBeInTheDocument();
  });

  it("pretty-prints a JSON response body", () => {
    render(
      <RequestDetails error={makeAxiosError({ status: 500, url: "/api/x", responseData: { message: "boom" } })} initiallyOpen={true} />,
    );
    expect(screen.getByText(/"message": "boom"/)).toBeInTheDocument();
  });

  it("truncates an oversized request body", () => {
    const bigBody = { blob: "x".repeat(3000) };
    render(<RequestDetails error={makeAxiosError({ status: 500, url: "/api/x", requestData: bigBody })} initiallyOpen={true} />);
    expect(screen.getByText(/truncated/)).toBeInTheDocument();
  });
});
