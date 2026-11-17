import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { ResponsiveTable } from "./ResponsiveTable";

interface Place {
  id: string;
  name: string;
  address: string;
}

const places: Place[] = [{ id: "p1", name: "Musterhaus", address: "Musterstr. 12, 12345 Musterstadt" }];

describe("ResponsiveTable", () => {
  /**
   * Regression coverage for #2244: a two-column table did not fill the container width.
   *
   * Root cause: the desktop show/hide toggle (`hidden @3xl:block`) was applied to the
   * `<table>` element itself. `@3xl:block` sets `display: block`, which overrides the
   * table's `display: table` and disables `table-fixed` + the `<colgroup>` percentage
   * widths, so the browser falls back to an auto-layout table that shrinks to content.
   *
   * The toggle must live on a wrapper element so the `<table>` keeps `display: table`.
   * jsdom does not compute layout, so this asserts the structural contract (which class
   * lands where); the rendered full-width behavior is covered by the Playwright story test.
   */
  it("keeps the responsive display toggle off the <table> element", () => {
    const { container } = render(
      <ResponsiveTable
        data={places}
        config={{
          columns: [
            { id: "name", header: "Name", cell: (item) => item.name },
            { id: "address", header: "Adresse", cell: (item) => item.address },
          ],
        }}
      />,
    );

    const table = container.querySelector("table");
    expect(table).not.toBeNull();

    const tableClasses = table?.className.split(/\s+/) ?? [];
    expect(tableClasses).toContain("table-fixed");
    expect(tableClasses).toContain("w-full");
    // A `display: block` / hidden utility on the <table> would break table layout.
    expect(tableClasses).not.toContain("@3xl:block");
    expect(tableClasses).not.toContain("block");
    expect(tableClasses).not.toContain("hidden");

    // The desktop toggle must instead live on an ancestor wrapper of the <table>.
    const desktopWrapper = container.querySelector(".\\@3xl\\:block");
    expect(desktopWrapper).not.toBeNull();
    expect(desktopWrapper?.contains(table)).toBe(true);
    expect(desktopWrapper).not.toBe(table);
  });

  it("puts rowClassName on the table row and on the card, per item", () => {
    const twoPlaces: Place[] = [...places, { id: "p2", name: "Zweithaus", address: "Andere Str. 3" }];
    const { container } = render(
      <ResponsiveTable
        data={twoPlaces}
        config={{
          rowClassName: (item) => (item.id === "p1" ? "font-semibold" : undefined),
          columns: [
            { id: "name", header: "Name", viewOnMobile: "primary-header", cell: (item) => item.name },
            { id: "address", header: "Adresse", cell: (item) => item.address },
          ],
        }}
      />,
    );

    const rows = [...container.querySelectorAll("tbody tr")];
    expect(rows[0]?.className).toContain("font-semibold");
    expect(rows[1]?.className).not.toContain("font-semibold");

    const cards = [...container.querySelectorAll(".\\@3xl\\:hidden > div > *")];
    expect(cards[0]?.className).toContain("font-semibold");
    expect(cards[1]?.className).not.toContain("font-semibold");
  });

  it("leaves out the card's label for a column that renders nothing for this item", () => {
    const { container } = render(
      <ResponsiveTable
        data={places}
        config={{
          columns: [
            { id: "name", header: "Name", viewOnMobile: "primary-header", cell: (item) => item.name },
            { id: "address", header: "Adresse", cell: (item) => item.address },
            { id: "amount", header: "Menge", cell: () => null },
          ],
        }}
      />,
    );

    const cardView = container.querySelector(".\\@3xl\\:hidden");
    expect(cardView?.textContent).toContain("Adresse");
    expect(cardView?.textContent).not.toContain("Menge");
    // The desktop table keeps the cell — a table row has to fill every column.
    expect(container.querySelectorAll("tbody tr td")).toHaveLength(3);
  });
});
