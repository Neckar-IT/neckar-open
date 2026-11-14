import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button, buttonVariants } from "./Button";

describe("Button", () => {
  /**
   * The button always renders two children — the content and the loading spinner — which
   * `Slot.Root` cannot merge on its own. Before `Slot.Slottable` was introduced, every `asChild`
   * button threw "Slot failed to slot onto its children" at render time and took the whole page
   * down with it. The bug stayed latent because no product used `Button asChild` until ideas.neckar.it
   * did, and it only surfaced in production.
   */
  it("renders as its child element without throwing when asChild is set", () => {
    render(
      <Button asChild={true}>
        <a href="/somewhere">Zum Ziel</a>
      </Button>,
    );

    const link = screen.getByRole("link", { name: "Zum Ziel" });
    expect(link).toBeVisible();
    expect(link.getAttribute("href")).toBe("/somewhere");
  });

  it("merges its classes onto the child element", () => {
    render(
      <Button asChild={true} className="custom-marker">
        <a href="/somewhere">Zum Ziel</a>
      </Button>,
    );

    expect(screen.getByRole("link", { name: "Zum Ziel" }).className).toContain("custom-marker");
  });

  it("keeps the child a single element even when the content is a plain string", () => {
    render(
      <Button asChild={true}>
        <a href="/somewhere">Nur Text</a>
      </Button>,
    );

    expect(screen.getByRole("link", { name: "Nur Text" })).toBeVisible();
  });

  it("still renders a real button element when asChild is not set", () => {
    render(<Button>Speichern</Button>);

    expect(screen.getByRole("button", { name: "Speichern" })).toBeVisible();
  });

  it("disables itself while loading", () => {
    render(<Button loading={true}>Speichern</Button>);

    expect(screen.getByRole("button", { name: "Speichern" })).toBeDisabled();
  });

  it("hides icon and text next to each other while loading, and shows both otherwise", () => {
    const { rerender } = render(
      <Button loading={true}>
        <svg data-testid="icon" />
        Ersetzen
      </Button>,
    );

    expect(screen.getByText("Ersetzen").closest(".invisible")).toContainElement(screen.getByTestId("icon"));

    rerender(
      <Button loading={false}>
        <svg data-testid="icon" />
        Ersetzen
      </Button>,
    );

    expect(screen.getByText("Ersetzen").closest(".invisible")).toBeNull();
  });

  it("hides mixed text nodes and elements while loading", () => {
    render(
      <Button loading={true}>
        {3} <b>Dateien</b>
      </Button>,
    );

    const hidden = screen.getByText("Dateien").closest(".invisible");
    expect(hidden).not.toBeNull();
    expect(hidden).toHaveTextContent("3 Dateien");
  });

  it("keeps the spinner outside the hidden content", () => {
    render(<Button loading={true}>Speichern</Button>);

    const spinner = screen.getByRole("button").querySelector("svg.animate-spin");
    expect(spinner).not.toBeNull();
    expect(spinner?.closest(".invisible")).toBeNull();
  });

  it("hides the text of an asChild child while loading", () => {
    render(
      <Button asChild={true} loading={true}>
        <a href="/somewhere">Zum Ziel</a>
      </Button>,
    );

    expect(screen.getByText("Zum Ziel").closest(".invisible")).not.toBeNull();
  });

  it("stays disabled while loading even with an explicit disabled={false}", () => {
    render(
      <Button loading={true} disabled={false}>
        Speichern
      </Button>,
    );

    expect(screen.getByRole("button")).toBeDisabled();
  });

  it("colors the confirm variant in the confirmed color instead of the primary one", () => {
    render(<Button variant="confirm">Bestätigen</Button>);

    const button = screen.getByRole("button", { name: "Bestätigen" });
    expect(button).toHaveClass("bg-confirmed", "hover:bg-confirmed/80");
    expect(button).not.toHaveClass("bg-primary");
    expect(button).toHaveAttribute("data-variant", "confirm");
  });
});

describe("buttonVariants", () => {
  it("styles an element as a confirm button", () => {
    const classes = buttonVariants({ variant: "confirm" }).split(" ");

    expect(classes).toContain("bg-confirmed");
    expect(classes).not.toContain("bg-primary");
  });
});
