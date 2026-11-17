/**
 * Imports
 */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { WithMemoryRouter } from "./WithMemoryRouter";

/**
 * Tests
 */

describe("WithMemoryRouter", () => {
  it("should render the children by default", () => {
    render(
      <WithMemoryRouter>
        <div>Some Text</div>
      </WithMemoryRouter>,
    );

    expect(screen.getByText("Some Text")).toBeInTheDocument();
  });

  it("should render the back link when the url does not match the path", () => {
    render(
      <WithMemoryRouter path="/some-path" url="/other-path">
        <div>Some Text</div>
      </WithMemoryRouter>,
    );

    expect(screen.getByText("Back")).toBeInTheDocument();
  });
});
