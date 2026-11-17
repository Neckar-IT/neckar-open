import { QueryStatus, type QueryResult } from "@neckar-hub/typescript-react-query";
import type { DataSliceController, DataSliceParams } from "@neckar-hub/typescript-react-utils/data-slice-filters";
import { DEFAULT_PAGE_SIZE } from "@neckar-hub/typescript-react-utils/search-params";
import type { PaginationInfo } from "@neckar-hub/typescript-utils/openapi-types";
import { isBadResponseType, unexpectedRestResponseTypeError } from "@neckar-hub/typescript-utils/rest";
import type { ReactElement, ReactNode } from "react";
import { BadResponseError } from "../../../query-handling/BadResponseError";
import { BadResponseFallback } from "../../../query-handling/BadResponseFallback";
import { ResponsiveTable, type ResponsiveTableConfig, SkeletonResponsiveTable } from "../../data-display/responsive-table";
import { DataSliceView } from "./DataSliceView";

/**
 * Generic shape every table-list REST endpoint produces in the `good` case: a paginated `data`
 * array plus filter-state echo. Generated OpenAPI schemas like `BookTableEntriesResponseGoodWithType`
 * structurally match this — the `FilterState` slot is left to each schema.
 */
interface TableResponseGood<ItemType, FilterState> {
  type: "good";
  data: ItemType[];
  paginationInfo: PaginationInfo;
  filterState?: FilterState;
}

interface TableResponseBad {
  type: `bad/${string}`;
}

export type TableResponse<ItemType, FilterState> = TableResponseGood<ItemType, FilterState> | TableResponseBad;

/**
 * Configuration for a [DataSliceTable]: extends [ResponsiveTableConfig] (columns, row link,
 * estimated row height, ...) with the `toolbar` slot above the table chrome.
 *
 * `toolbar` renders a `DataSliceToolbar`; it receives the `filterState` the response echoes, which an entity filter
 * reads its preview from, and `undefined` while the response is pending.
 */
export type DataSliceTableConfig<ItemType, SortCriteria, FilterState = unknown> = ResponsiveTableConfig<ItemType, SortCriteria> & {
  toolbar?: (filterState: FilterState | undefined) => ReactNode;
  empty?: ReactNode;
};

/** The part of a `DataSliceController` the table reads and writes. */
type TableController<SortCriteria> = Pick<
  DataSliceController<DataSliceParams<SortCriteria>, SortCriteria>,
  "params" | "setPagination" | "setSorting"
>;

export interface DataSliceTableProps<ItemType, SortCriteria, FilterState = unknown> {
  query: QueryResult<TableResponse<ItemType, FilterState>>;
  dataSliceController: TableController<SortCriteria>;
  config: DataSliceTableConfig<ItemType, SortCriteria, FilterState>;
  pageSizeOptions?: number[];
  className?: string;
}

/**
 * Full lifecycle wrapper for a table-driven [DataSliceView]: covers `Pending` (skeleton),
 * `Fetching` / `Success` with a `good` response (resolved table), and `Fetching` / `Success` with
 * a `bad/*` response (`BadResponseFallback`); a `good` response without any record shows `config.empty`.
 * Replaces the recurring three-branch `switch` in every patterns table.
 */
export function DataSliceTable<ItemType, SortCriteria, FilterState = unknown>({
  query,
  dataSliceController,
  config,
  pageSizeOptions,
  className,
}: DataSliceTableProps<ItemType, SortCriteria, FilterState>): ReactElement {
  const rowCount = dataSliceController.params.pageSize ?? DEFAULT_PAGE_SIZE;
  const { toolbar, empty } = config;

  switch (query.status) {
    case QueryStatus.Pending:
      // Render `DataSliceView` directly — same component type as the resolved branch below — so
      // React reconciliation keeps the toolbar subtree mounted across the Pending ↔ Success
      // transition. Otherwise stateful filter children (open popovers, in-flight combobox edits)
      // would unmount on every URL-driven refetch.
      return (
        <DataSliceView className={className} pending toolbar={toolbar?.(undefined)}>
          <SkeletonResponsiveTable config={config} rowCount={rowCount} />
        </DataSliceView>
      );
    case QueryStatus.Fetching:
    case QueryStatus.Success: {
      const response = query.response;
      if (response.type === "good") {
        return (
          <DataSliceView
            className={className}
            paginationInfo={response.paginationInfo}
            onPaginationParamsChange={dataSliceController.setPagination}
            pageSizeOptions={pageSizeOptions}
            toolbar={toolbar?.(response.filterState)}
            empty={empty}
          >
            <ResponsiveTable
              data={response.data}
              config={config}
              sortBy={dataSliceController.params.sortBy}
              sortOrder={dataSliceController.params.sortOrder}
              onSortingParamsChange={dataSliceController.setSorting}
            />
          </DataSliceView>
        );
      }
      if (isBadResponseType(response.type)) {
        return <BadResponseFallback error={new BadResponseError({ badType: response.type })} />;
      }
      throw unexpectedRestResponseTypeError(response.type);
    }
    default:
      throw new Error(`Unexpected value in Switch Statement: query.status`);
  }
}
