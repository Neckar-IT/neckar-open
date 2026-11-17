import type { QueryResult } from "@neckar-hub/typescript-react-query";
import type { DataSliceController, DataSliceParams } from "@neckar-hub/typescript-react-utils/data-slice-filters";
import type { Uuid } from "@neckar-hub/typescript-utils/types";
import type { ReactElement, ReactNode } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { type ResponsiveColumnDefinition, selectEntityColumn } from "../../data-display/responsive-table";
import { Filters, type FiltersProps } from "../../filters/table-filters";
import { HighlightSource } from "../../text/highlight-text";
import { DataSliceTable, type DataSliceTableConfig, type DataSliceTableProps, type TableResponse } from "./DataSliceTable";
import { DataSliceToolbar } from "./DataSliceToolbar";

export interface EntitySelectTableProps<ItemType extends { id: Uuid }, SortCriteria, FilterState = unknown> {
  query: QueryResult<TableResponse<ItemType, FilterState>>;
  dataSliceController: DataSliceTableProps<ItemType, SortCriteria, FilterState>["dataSliceController"] &
    Pick<DataSliceController<DataSliceParams<SortCriteria>, SortCriteria>, "freeTextTokens">;
  /** The columns before the select button, without links: a link would leave the form that opened the dialog. */
  columns: ResponsiveColumnDefinition<ItemType, SortCriteria>[];
  filters: FiltersProps;
  /** The ids already picked — a row whose id is among them is marked as selected. */
  selectedIds: Uuid[];
  onSelect: (value: ItemType) => void;
  /** The actions at the end of the toolbar, e.g. the button that creates an entity from the typed free text. */
  actions?: ReactNode;
}

/** The list inside an entity select dialog: the passed columns, a select button per row and a toolbar with the passed filters. */
export function EntitySelectTable<ItemType extends { id: Uuid }, SortCriteria, FilterState = unknown>({
  query,
  dataSliceController,
  columns,
  filters,
  selectedIds,
  onSelect,
  actions,
}: EntitySelectTableProps<ItemType, SortCriteria, FilterState>): ReactElement {
  const { t } = useEmbeddedTranslation();

  const config: DataSliceTableConfig<ItemType, SortCriteria, FilterState> = {
    columns: [
      ...columns,
      selectEntityColumn({
        isSelected: (item) => selectedIds.includes(item.id),
        onSelect,
        selectLabel: t("entitySelectTable.select"),
      }),
    ],
    toolbar: () => <DataSliceToolbar filters={<Filters {...filters} />} actions={actions} />,
  };

  return (
    <HighlightSource tokens={dataSliceController.freeTextTokens}>
      <DataSliceTable query={query} dataSliceController={dataSliceController} config={config} pageSizeOptions={[10]} />
    </HighlightSource>
  );
}
