import { render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import type { Action } from "../../actions/Action";
import { EmptyState, useNoEntitiesLabel } from "./EmptyState";

function NoContraptions() {
  return <EmptyState label={useNoEntitiesLabel("Contraptions")} />;
}

describe("EmptyState", () => {
  it("shows the action button after the label", async () => {
    const action: Action = { icon: undefined, label: "Create", onTrigger: vi.fn() };
    render(<EmptyState label="Nothing here" action={action} />);

    const label = screen.getByText("Nothing here");
    const button = screen.getByRole("button", { name: "Create" });
    expect(label.compareDocumentPosition(button) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("shows no button without an action", () => {
    render(<EmptyState label="Nothing here" />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("puts the icon before the label", () => {
    const { container } = render(<EmptyState label="Nothing here" />);

    expect(container.firstElementChild?.firstElementChild?.tagName.toLowerCase()).toBe("svg");
  });

  it("names the entities in the no-entities label", () => {
    render(<NoContraptions />);

    expect(screen.getByText("There are no Contraptions yet.")).toBeInTheDocument();
  });
});
