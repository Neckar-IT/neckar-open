import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { TooltipProvider } from "../../primitives/tooltip";
import { SingleLineList } from "./SingleLineList";

/** The app wraps every page in a `TooltipProvider`; the tests stand in for it. */
function renderInApp(ui: React.ReactElement): ReturnType<typeof render> {
  return render(<TooltipProvider>{ui}</TooltipProvider>);
}

describe("SingleLineList", () => {
  it("shows the first entry and counts the others on the badge", () => {
    renderInApp(<SingleLineList items={["first", "second", "third"]} />);

    expect(screen.getByText("first")).toBeInTheDocument();
    expect(screen.getByText("+2")).toHaveAccessibleName("2 more");
    expect(screen.queryByText("second")).not.toBeInTheDocument();
  });

  it("shows no badge for a single entry", () => {
    renderInApp(<SingleLineList items={["only"]} />);

    expect(screen.getByText("only")).toBeInTheDocument();
    expect(screen.queryByText(/^\+/)).not.toBeInTheDocument();
  });

  it("renders nothing for an empty or absent list", () => {
    const { container } = renderInApp(
      <>
        <SingleLineList items={[]} />
        <SingleLineList items={null} />
      </>,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it("lists every entry and nothing else in one tooltip over the line, each on one line", async () => {
    renderInApp(<SingleLineList items={["first", "second", "third"]} />);

    await userEvent.hover(screen.getByText("first"));

    await screen.findByRole("tooltip");
    expect(document.querySelector("[data-slot=tooltip-content] ul")?.parentElement).toHaveAttribute("data-slot", "tooltip-content");
    const entries = document.querySelectorAll("[data-slot=tooltip-content] li");
    expect([...entries].map((entry) => entry.textContent)).toEqual(["first", "second", "third"]);
    entries.forEach((entry) => expect(entry).toHaveClass("whitespace-nowrap"));
  });

  it("shows a single entry in the tooltip too", async () => {
    renderInApp(<SingleLineList items={["only"]} />);

    await userEvent.hover(screen.getByText("only"));

    expect(await screen.findByRole("tooltip")).toHaveTextContent("only");
  });

  it("opens the tooltip on keyboard focus", async () => {
    renderInApp(<SingleLineList items={["first", "second"]} />);

    await userEvent.tab();

    expect(await screen.findByRole("tooltip")).toHaveTextContent("second");
  });
});
