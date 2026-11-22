import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { QueryStatus, type QueryResult } from "@neckar-hub/typescript-react-query";
import { useDataSliceControllerFromState } from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { NowProvider } from "@neckar-hub/typescript-react-utils/providers";
import { type PriceTimelineEntriesFilterCriteriaState, ValueTimelineEntryStatus } from "@neckar-hub/typescript-utils/openapi-types";
import { render, screen } from "@test/test-utils";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import type { Action } from "../actions";
import type { TableResponse } from "../layout/data-slice-view";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import { type TimelineDataSliceParams, timelineParamDefinition } from "./timelineDataSliceController";
import { ValueTimelineTable } from "./ValueTimelineTable";

const now = new Date("2026-06-01T12:00:00Z");
const current: TimelineEntryLike = { validFrom: "2026-05-01T00:00:00Z" };
const future: TimelineEntryLike = { validFrom: "2026-07-01T00:00:00Z" };
const summary: TimelineSummaryLike<TimelineEntryLike> = { currentEntry: current, nextFutureEntry: future };
const addEntry: Action = { icon: undefined, label: "Add price", onTrigger: vi.fn() };

const query: QueryResult<TableResponse<TimelineEntryLike, PriceTimelineEntriesFilterCriteriaState>> = {
  status: QueryStatus.Success,
  response: {
    type: "good",
    data: [future, current],
    paginationInfo: { pageIndex: 0, pageSize: 10, pageCount: 1, filteredElementsCount: 2, totalElementsCount: 2 },
  },
  meta: undefined,
  queryKey: ["price-timeline"],
};

function PriceTimeline({ initialValues }: { initialValues?: Partial<TimelineDataSliceParams> }): React.ReactElement {
  const controller = useDataSliceControllerFromState<TimelineDataSliceParams>(timelineParamDefinition, initialValues);
  return (
    <NowProvider value={now}>
      <ValueTimelineTable
        summary={summary}
        toolbarStart={<p>Current price: 12.00 €</p>}
        query={query}
        valueColumns={[]}
        addEntryAction={addEntry}
        dataSliceController={controller}
        rowActions={() => null}
      />
      <button type="button" onClick={() => controller.setPagination({ pageIndex: 2, pageSize: 10 })}>
        Go to page 3
      </button>
      <output data-testid="params">{JSON.stringify(controller.params)}</output>
    </NowProvider>
  );
}

function params(): Record<string, unknown> {
  return JSON.parse(screen.getByTestId("params").textContent) as Record<string, unknown>;
}

describe("ValueTimelineTable", () => {
  it("shows the summary, the add action, the filters Valid from and Status in the filter group and the status of each row", () => {
    render(<PriceTimeline />);

    expect(screen.getByText("Current price: 12.00 €")).toBeInTheDocument();
    const filters = screen.getByRole("group", { name: "Filters" });
    expect(filters).toContainElement(screen.getByRole("button", { name: "Valid from" }));
    expect(filters).toContainElement(screen.getByRole("button", { name: "Status" }));
    expect(screen.getByRole("button", { name: "Add price" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Current" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Future" })).toBeInTheDocument();
  });

  it("picks a status, goes back to the first page and clears the status through its X", async () => {
    const user = userEvent.setup();
    render(<PriceTimeline />);
    await user.click(screen.getByRole("button", { name: "Go to page 3" }));

    await user.click(screen.getByRole("button", { name: "Status" }));
    await user.click(screen.getByRole("option", { name: "Future" }));

    expect(params()).toMatchObject({ status: ValueTimelineEntryStatus.Future, pageIndex: 0 });
    await user.click(screen.getByRole("button", { name: "Remove Status" }));
    expect(params().status).toBeUndefined();
  });

  it("sets validFromAfter to the start of the picked day and goes back to the first page", async () => {
    // The calendar opens on the month of the system clock.
    vi.useFakeTimers({ toFake: ["Date"], now });
    onTestFinished(() => void vi.useRealTimers());
    const user = userEvent.setup();
    render(<PriceTimeline />);
    await user.click(screen.getByRole("button", { name: "Go to page 3" }));

    await user.click(screen.getByRole("button", { name: "Valid from" }));
    await user.click(screen.getByRole("button", { name: "Valid from from" }));
    await user.click(screen.getByRole("button", { name: "Wednesday, June 10th, 2026" }));

    expect(params()).toMatchObject({ validFromAfter: new Date(2026, 5, 10).toISOString(), pageIndex: 0 });
  });

  it("names the picked status with the text of this library when the app translates a flat enum key of the same value", async () => {
    const user = userEvent.setup();
    render(
      <I18nProvider languageResources={{ de: { translation: { enums: { Current: "Strom" } } } }} options={{ lng: "de" }}>
        <PriceTimeline />
      </I18nProvider>,
    );

    await user.click(screen.getByRole("button", { name: "Status" }));

    await user.click(screen.getByRole("option", { name: "Aktuell" }));

    expect(screen.getByRole("button", { name: "Status: Aktuell" })).toBeInTheDocument();
  });

  it("shows a set validFromAfter on its button, clears it through its X and goes back to the first page", async () => {
    const user = userEvent.setup();
    render(<PriceTimeline initialValues={{ validFromAfter: "2026-01-01T00:00:00.000Z" }} />);
    await user.click(screen.getByRole("button", { name: "Go to page 3" }));

    expect(screen.getByRole("button", { name: /^Valid from: / })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Remove Valid from" }));

    expect(params().validFromAfter).toBeUndefined();
    expect(params()).toMatchObject({ pageIndex: 0 });
  });
});
