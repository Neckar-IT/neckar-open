import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import { ResponsiveTable } from "./ResponsiveTable";
import { selectEntityColumn } from "./selectEntityColumn";

interface Row {
  id: string;
  name: string;
}

const firstRow: Row = { id: "r1", name: "First" };
const secondRow: Row = { id: "r2", name: "Second" };

/**
 * `ResponsiveTable` renders the desktop table and the mobile card view at the same time, so every
 * row contributes two select buttons. The assertions therefore count buttons instead of picking one.
 */
function renderTable({ data, isSelected, onSelect }: { data: Row[]; isSelected: (row: Row) => boolean; onSelect: (row: Row) => void }) {
  return render(
    <ResponsiveTable
      data={data}
      config={{
        columns: [
          { id: "name", header: "Name", cell: (row) => row.name },
          selectEntityColumn<Row, never>({ isSelected, onSelect, selectLabel: "Select" }),
        ],
      }}
    />,
  );
}

function selectButtons(): HTMLButtonElement[] {
  return screen.getAllByRole<HTMLButtonElement>("button", { name: "Select" });
}

describe("selectEntityColumn", () => {
  it("uses the column id `select`", () => {
    const column = selectEntityColumn<Row, never>({ isSelected: () => false, onSelect: () => {}, selectLabel: "Select" });

    expect(column.id).toBe("select");
  });

  it("aligns the button to the right of its cell", () => {
    renderTable({ data: [firstRow], isSelected: () => false, onSelect: () => {} });

    expect(selectButtons().every((button) => button.closest(".text-right") !== null)).toBe(true);
  });

  it("renders into the mobile card header, not as a detail row against an empty label", () => {
    const column = selectEntityColumn<Row, never>({ isSelected: () => false, onSelect: () => {}, selectLabel: "Select" });
    expect(column.viewOnMobile).toBe("secondary-header");

    const { container } = renderTable({ data: [firstRow], isSelected: () => false, onSelect: () => {} });

    // The name column is the only one left in the card's detail list; a headerless select column
    // placed there would add a second, empty `<dt>`.
    expect(Array.from(container.querySelectorAll("dt")).map((term) => term.textContent)).toEqual(["Name"]);
  });

  it("reports the clicked row to onSelect", () => {
    const onSelect = vi.fn();
    renderTable({ data: [firstRow], isSelected: () => false, onSelect });

    fireEvent.click(elementAt(selectButtons(), 0));

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith(firstRow);
  });

  it("disables the button of a row the predicate reports as selected", () => {
    renderTable({ data: [firstRow], isSelected: () => true, onSelect: () => {} });

    expect(selectButtons().every((button) => button.disabled)).toBe(true);
  });

  it("keeps the buttons of the remaining rows enabled", () => {
    const onSelect = vi.fn();
    renderTable({ data: [firstRow, secondRow], isSelected: (row) => row.id === firstRow.id, onSelect });

    const enabledButtons = selectButtons().filter((button) => !button.disabled);
    expect(enabledButtons).toHaveLength(2);

    fireEvent.click(elementAt(enabledButtons, 0));

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith(secondRow);
  });
});
