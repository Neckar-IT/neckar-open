import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { Alert, AlertTitle } from "./Alert";

describe("Alert", () => {
  it("colors the info variant in the primary color over the card color", () => {
    render(
      <Alert variant="info">
        <AlertTitle>Hinweis</AlertTitle>
      </Alert>,
    );

    const alert = screen.getByRole("alert");
    expect(alert).toHaveClass("text-primary", "border-primary/50");
    expect(alert).not.toHaveClass("text-card-foreground");
  });

  it("sets a muted background on the muted variant", () => {
    render(<Alert variant="muted">Anmerkung</Alert>);

    const alert = screen.getByRole("alert");
    expect(alert).toHaveClass("bg-muted/50", "text-muted-foreground");
    expect(alert).not.toHaveClass("bg-card");
  });

  it("keeps the primitive's destructive color", () => {
    render(<Alert variant="destructive">Fehler</Alert>);

    expect(screen.getByRole("alert")).toHaveClass("text-destructive");
  });
});
