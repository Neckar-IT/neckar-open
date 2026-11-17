import type { QueryResult } from "@neckar-hub/typescript-react-query";
import { useNow } from "@neckar-hub/typescript-react-utils/providers";
import type { PriceTimelineEntriesFilterCriteriaState } from "@neckar-hub/typescript-utils/openapi-types";
import type { ReactNode } from "react";
import { ActionButton, type Action } from "../actions";
import type { ResponsiveColumnDefinition } from "../data-display/responsive-table";
import {
  DataSliceTable,
  type DataSliceTableConfig,
  DataSliceToolbar,
  type DataSliceParams,
  type TableResponse,
} from "../layout/data-slice-view";
import type { TimelineEntryLike, TimelineSummaryLike } from "./timeline.types";
import type { TimelineDataSliceController } from "./timelineDataSliceController";
import { rowActionsColumn, statusColumn, type TimelineSortKey, validFromColumn } from "./timelineEntryTableColumns";

type ValueTimelineTableProps<TEntry extends TimelineEntryLike> = {
  className?: string;
  /** Its entries may carry less than a row: they only mark which row is in effect. */
  summary: TimelineSummaryLike<TimelineEntryLike>;
  /**
   * Rendered at the start of the table toolbar, opposite the add-entry action.
   * The timeline pages pass their summary headline here so the current-value summary shares the
   * toolbar row with the add button.
   */
  toolbarStart?: ReactNode;
  query: QueryResult<TableResponse<TEntry, PriceTimelineEntriesFilterCriteriaState>>;
  /**
   * The columns that render the entry's value, inserted between the `validFrom` and the
   * row-actions column. This is the only part of the table that depends on the value type — a
   * single price column for a price timeline, several for a structured value.
   */
  valueColumns: ResponsiveColumnDefinition<TEntry, TimelineSortKey>[];
  addEntryAction: Action;
  dataSliceController: TimelineDataSliceController<DataSliceParams<TimelineSortKey>>;
  /** Renders the actions of a future-dated row; see [rowActionsColumn]. */
  rowActions: (entry: TEntry) => ReactNode;
};

/**
 * Status, `validFrom` and the future-only row-actions column come from this component; the value
 * columns and the actions of a row are passed in by the instantiation.
 */
export function ValueTimelineTable<TEntry extends TimelineEntryLike>({
  className,
  summary,
  toolbarStart,
  query,
  valueColumns,
  addEntryAction,
  dataSliceController,
  rowActions,
}: ValueTimelineTableProps<TEntry>): React.ReactElement {
  const now = useNow();

  const config: DataSliceTableConfig<TEntry, TimelineSortKey, PriceTimelineEntriesFilterCriteriaState> = {
    columns: [statusColumn({ summary, now }), validFromColumn(), ...valueColumns, rowActionsColumn({ summary, now, rowActions })],
    // Bottom-align the action to the (taller) summary at the start of the toolbar so both share one baseline
    // and the same gap above the table, instead of the button floating at the top of the toolbar row.
    toolbar: () => (
      <DataSliceToolbar
        filters={toolbarStart}
        actions={<ActionButton action={addEntryAction} variant="default" className="@3xl:self-end" />}
      />
    ),
  };

  return <DataSliceTable query={query} dataSliceController={dataSliceController} config={config} className={className} />;
}
