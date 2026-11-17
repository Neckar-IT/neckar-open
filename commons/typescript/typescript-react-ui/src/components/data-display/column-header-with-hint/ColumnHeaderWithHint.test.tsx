import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ColumnHeaderWithHint } from "./ColumnHeaderWithHint";

describe("ColumnHeaderWithHint", () => {
  it("shows the hint only after a click on the info icon", async () => {
    const user = userEvent.setup();
    render(<ColumnHeaderWithHint hint="All amounts net.">Price</ColumnHeaderWithHint>);

    expect(screen.getByText("Price")).toBeInTheDocument();
    expect(screen.queryByText("All amounts net.")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Show hint" }));

    expect(screen.getByText("All amounts net.")).toBeInTheDocument();
  });
});
