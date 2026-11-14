import { QueryStatus, type QueryResult } from "@neckar-hub/typescript-react-query";
import type { DataSliceParams } from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { render, screen } from "@test/test-utils";
import { describe, expect, it, vi } from "vitest";
import type { Action } from "../../actions/Action";
import { EmptyState, useNoEntitiesLabel } from "../../feedback/empty-state";
import { DataSliceTable, type DataSliceTableConfig, type TableResponse } from "./DataSliceTable";
import { DataSliceToolbar } from "./DataSliceToolbar";

interface Contraption {
  id: string;
  name: string;
}

const dataSliceController = {
  params: {} satisfies DataSliceParams<never>,
  setPagination: vi.fn(),
  setSorting: vi.fn(),
};

const addContraption: Action = { icon: undefined, label: "Contraption anlegen", onTrigger: vi.fn() };

function NoContraptions() {
  return <EmptyState label={useNoEntitiesLabel("Contraptions")} action={addContraption} />;
}

const config: DataSliceTableConfig<Contraption, never> = {
  columns: [{ id: "name", header: "Name", cell: (contraption) => contraption.name }],
  empty: <NoContraptions />,
};

function queryOf(data: Contraption[], totalElementsCount: number): QueryResult<TableResponse<Contraption, unknown>> {
  return {
    status: QueryStatus.Success,
    response: {
      type: "good",
      data,
      paginationInfo: { pageIndex: 0, pageSize: 10, pageCount: 1, filteredElementsCount: data.length, totalElementsCount },
    },
    meta: undefined,
    queryKey: ["contraptions"],
  };
}

describe("DataSliceTable", () => {
  it("shows the passed empty node while no record exists", () => {
    render(<DataSliceTable query={queryOf([], 0)} dataSliceController={dataSliceController} config={config} />);

    expect(screen.getByText("There are no Contraptions yet.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Contraption anlegen" })).toBeEnabled();
  });

  it("shows the filter hint without the create action while the filter hides every record", () => {
    render(<DataSliceTable query={queryOf([], 3)} dataSliceController={dataSliceController} config={config} />);

    expect(screen.getByText("No Items match your filters.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Contraption anlegen" })).not.toBeInTheDocument();
  });

  it("shows the generic hint for a config without an empty state", () => {
    render(<DataSliceTable query={queryOf([], 0)} dataSliceController={dataSliceController} config={{ columns: config.columns }} />);

    expect(screen.getByText("There is no data available yet.")).toBeInTheDocument();
  });

  it("shows the records and no hint while records exist", () => {
    render(
      <DataSliceTable
        query={queryOf([{ id: "1", name: "Flux Capacitor" }], 1)}
        dataSliceController={dataSliceController}
        config={config}
      />,
    );

    expect(screen.getAllByText("Flux Capacitor").length).toBeGreaterThan(0);
    expect(screen.queryByText("There are no Contraptions yet.")).not.toBeInTheDocument();
  });

  it("hands the filter state of the response to the toolbar and shows the actions", () => {
    const query: QueryResult<TableResponse<Contraption, { customer: string }>> = {
      status: QueryStatus.Success,
      response: {
        type: "good",
        data: [{ id: "1", name: "Flux Capacitor" }],
        paginationInfo: { pageIndex: 0, pageSize: 10, pageCount: 1, filteredElementsCount: 1, totalElementsCount: 1 },
        filterState: { customer: "Jonas Weber" },
      },
      meta: undefined,
      queryKey: ["contraptions"],
    };
    const toolbarConfig: DataSliceTableConfig<Contraption, never, { customer: string }> = {
      columns: config.columns,
      toolbar: (filterState) => (
        <DataSliceToolbar
          filters={<span>Customer: {filterState?.customer ?? "pending"}</span>}
          actions={<button type="button">Anlegen</button>}
        />
      ),
    };

    render(<DataSliceTable query={query} dataSliceController={dataSliceController} config={toolbarConfig} />);

    expect(screen.getByText("Customer: Jonas Weber")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anlegen" })).toBeInTheDocument();
  });

  it("renders the toolbar without a filter state while the query is pending", () => {
    const pending: QueryResult<TableResponse<Contraption, { customer: string }>> = {
      status: QueryStatus.Pending,
      response: undefined,
      meta: undefined,
      queryKey: ["contraptions"],
    };
    const toolbarConfig: DataSliceTableConfig<Contraption, never, { customer: string }> = {
      columns: config.columns,
      toolbar: (filterState) => <DataSliceToolbar filters={<span>Customer: {filterState?.customer ?? "pending"}</span>} />,
    };

    render(<DataSliceTable query={pending} dataSliceController={dataSliceController} config={toolbarConfig} />);

    expect(screen.getByText("Customer: pending")).toBeInTheDocument();
  });
});
