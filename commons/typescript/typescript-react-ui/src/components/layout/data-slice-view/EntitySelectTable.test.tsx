import { QueryStatus, type QueryResult } from "@neckar-hub/typescript-react-query";
import type { Uuid } from "@neckar-hub/typescript-utils/types";
import { elementAt, render, screen, within } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { TableFilter } from "../../filters/table-filters";
import { HighlightText } from "../../text/highlight-text";
import type { TableResponse } from "./DataSliceTable";
import { EntitySelectTable } from "./EntitySelectTable";

interface Contraption {
  id: Uuid;
  name: string;
}

const fluxCapacitor: Contraption = { id: "a1b2c3d4-0000-4000-8000-000000000001" as Uuid, name: "Flux Capacitor" };
const timeCircuit: Contraption = { id: "a1b2c3d4-0000-4000-8000-000000000002" as Uuid, name: "Time Circuit" };

const query: QueryResult<TableResponse<Contraption, unknown>> = {
  status: QueryStatus.Success,
  response: {
    type: "good",
    data: [fluxCapacitor, timeCircuit],
    paginationInfo: { pageIndex: 0, pageSize: 10, pageCount: 1, filteredElementsCount: 2, totalElementsCount: 2 },
  },
  meta: undefined,
  queryKey: ["contraptions"],
};

const searchFilter: TableFilter = {
  key: "freeText",
  label: "Search contraptions",
  active: false,
  render: ({ label }) => <button type="button">{label}</button>,
};

/** `ResponsiveTable` renders the desktop table and the mobile cards at once; the assertions read the desktop table. */
function desktopSelectButtons(): HTMLButtonElement[] {
  return within(screen.getByRole("table")).getAllByRole<HTMLButtonElement>("button", { name: "Select" });
}

function renderTable({
  selectedIds = [],
  onSelect = vi.fn(),
  freeTextTokens,
  actions,
}: {
  selectedIds?: Uuid[];
  onSelect?: (value: Contraption) => void;
  freeTextTokens?: string[];
  actions?: React.ReactNode;
}) {
  return render(
    <EntitySelectTable<Contraption, never>
      query={query}
      dataSliceController={{ params: {}, setPagination: vi.fn(), setSorting: vi.fn(), freeTextTokens }}
      columns={[{ id: "name", header: "Name", cell: (contraption) => <HighlightText text={contraption.name} /> }]}
      filters={{ primary: [searchFilter] }}
      selectedIds={selectedIds}
      onSelect={onSelect}
      actions={actions}
    />,
  );
}

describe("EntitySelectTable", () => {
  it("renders the passed columns and a select button per row", () => {
    renderTable({});

    expect(screen.getAllByText("Flux Capacitor").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Time Circuit").length).toBeGreaterThan(0);
    expect(desktopSelectButtons()).toHaveLength(2);
  });

  it("disables the select button of a row whose id is already picked", () => {
    renderTable({ selectedIds: [fluxCapacitor.id] });

    expect(elementAt(desktopSelectButtons(), 0)).toBeDisabled();
    expect(elementAt(desktopSelectButtons(), 1)).toBeEnabled();
  });

  it("reports the row whose select button is clicked", async () => {
    const onSelect = vi.fn();
    renderTable({ onSelect });

    await userEvent.click(elementAt(desktopSelectButtons(), 1));

    expect(onSelect).toHaveBeenCalledWith(timeCircuit);
  });

  it("renders the primary filter in the toolbar", () => {
    renderTable({});

    expect(screen.getByRole("button", { name: "Search contraptions" })).toBeInTheDocument();
  });

  it("renders the passed actions", () => {
    renderTable({ actions: <button type="button">Add contraption</button> });

    expect(screen.getByRole("button", { name: "Add contraption" })).toBeInTheDocument();
  });

  it("highlights the free-text tokens of the controller in the naming column", () => {
    const { container } = renderTable({ freeTextTokens: ["Flux"] });

    expect([...container.querySelectorAll("mark")].map((mark) => mark.textContent)).toContain("Flux");
  });
});
