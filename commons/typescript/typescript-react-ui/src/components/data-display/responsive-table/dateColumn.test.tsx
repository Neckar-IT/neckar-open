import { render } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { dateColumn, dateTimeColumn } from "./dateColumn";
import { ResponsiveTable } from "./ResponsiveTable";

interface Row {
  id: string;
  releaseDate: string;
  createdAt: string;
}

const rows: Row[] = [{ id: "r1", releaseDate: "2026-04-01", createdAt: "2026-04-01T08:30:00Z" }];

describe("dateColumn", () => {
  it("renders a LocalDate accessor value via FormattedDate", () => {
    const { container } = render(
      <ResponsiveTable
        data={rows}
        config={{
          columns: [
            dateColumn<Row, never>({
              id: "releaseDate",
              header: "Release",
              accessor: (row) => row.releaseDate,
            }),
          ],
        }}
      />,
    );
    const text = container.textContent ?? "";
    expect(text).toMatch(/2026/);
    expect(text).toMatch(/04|4\b|Apr/);
  });

  it("forwards width and class options to the column definition", () => {
    const column = dateColumn<Row, never>({
      id: "releaseDate",
      header: "Release",
      accessor: (row) => row.releaseDate,
      cellClassName: "text-right",
      fixedWidth: "120px",
    });
    expect(column.cellClassName).toBe("text-right");
    expect("fixedWidth" in column ? column.fixedWidth : undefined).toBe("120px");
  });
});

describe("dateTimeColumn", () => {
  it("renders an Instant accessor value via FormattedDateTime", () => {
    const { container } = render(
      <ResponsiveTable
        data={rows}
        config={{
          columns: [
            dateTimeColumn<Row, never>({
              id: "createdAt",
              header: "Created",
              accessor: (row) => row.createdAt,
            }),
          ],
        }}
      />,
    );
    expect(container.textContent ?? "").toMatch(/2026/);
  });
});
