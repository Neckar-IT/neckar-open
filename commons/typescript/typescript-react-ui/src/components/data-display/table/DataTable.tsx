import { flexRender, type Renderable } from "@neckar-hub/typescript-react-utils/render";
import type { OnSortingParamsChange, SortingParams } from "@neckar-hub/typescript-react-utils/search-params";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { Link } from "react-router";
import { SortingToggle, type SortingIconVariant } from "../../actions/sorting-toggle";
import { Table as TableRoot, TableHeader, TableRow, TableHead, TableBody, TableCell } from "../../primitives/table";
import { resolveRowKey } from "./resolveRowKey";

export interface HeaderContext<SortCriteria = unknown> {
  column: {
    id: string;
    sortBy?: SortCriteria;
  };
  sorting: SortingParams<SortCriteria> | undefined;
}

/**
 * By default the table will distribute the columns evenly (relativeWidth = 1).
 * You have two options to customize the column widths:
 * 1. fixedWidth: Specify a fixed width (as absolute CSS value, e.g., "100px", "20%") for the column. This will make the column take up exactly that much space.
 * 2. relativeWidth:
 */
type ColumnWidthProps =
  | { fixedWidth: string; relativeWidth?: never }
  | { relativeWidth: number; fixedWidth?: never }
  | { fixedWidth?: never; relativeWidth?: never };

/**
 * Defines a column in the table
 */
export type ColumnDefinition<ItemType, SortCriteria = never> = {
  id: string;
  header?: Renderable<HeaderContext>;
  headerClassName?: string;
  /**
   * The enum property, which is used to sort this column. If provided, the header will render a button that allows sorting by this column.
   */
  sortBy?: SortCriteria;
  sortingIconVariant?: SortingIconVariant;
  cell: (item: ItemType) => React.ReactNode;
  cellClassName?: string;
} & ColumnWidthProps;

export type TableConfig<ItemType, SortCriteria = never> = {
  rowLink?: (item: ItemType) => string;
  columns: ColumnDefinition<ItemType, SortCriteria>[];
  /**
   * Classes for the row itself, so a table whose rows are not all of one kind can set one off.
   * `cellClassName` lands inside a cell and reaches neither the row background nor its borders.
   */
  rowClassName?: (item: ItemType, index: number) => string | undefined;
  /**
   * Optional override for the row height in pixels — used by skeleton/loading renderers to match the
   * height of real rows so there is no layout shift when data resolves. Leave unset to use the
   * default row height (`TableCell` uses `h-14` = 56px). Set this only when row content forces a
   * larger height (e.g. multi-line cells).
   */
  estimatedRowHeight?: number;
};

export interface TableProps<ItemType, SortCriteria = never> {
  /**
   * An array of objects to be rendered as rows in the table.
   *
   * Please make sure, that every item has an `id: string` property, which can be used as key for the rows. Otherwise the index is used,
   * which can lead to performance issues and bugs when the data changes.
   */
  data: ItemType[];
  config: TableConfig<ItemType, SortCriteria>;
  /**
   * The enum property, which currently sorts the data in the table.
   */
  sortBy?: SortCriteria;
  /**
   * The current sort order
   */
  sortOrder?: SortOrder;
  /**
   * Callback function that is triggered when the user changes the sorting by clicking on the sorting toggle button in the column header.
   */
  onSortingParamsChange?: OnSortingParamsChange<SortCriteria>;
  className?: string;
}

/**
 * DataTable component where you pass in the data and columnDefinitions and it will render the table.
 */
export function DataTable<ItemType, SortCriteria>({
  data,
  config,
  sortBy,
  sortOrder,
  onSortingParamsChange,
  className,
}: TableProps<ItemType, SortCriteria>): React.ReactElement {
  const hasHeaders = config.columns.some((column) => column.header !== undefined);

  const totalRelative = config.columns.reduce((sum, col) => (col.fixedWidth === undefined ? sum + (col.relativeWidth ?? 1) : sum), 0);

  const sortingParams: SortingParams<SortCriteria> | undefined = isDefined(sortBy)
    ? { sortBy: sortBy, sortOrder: sortOrder ?? SortOrder.Asc }
    : undefined;

  // `min-w-0 overflow-hidden` on header and body wrappers clips cell content at the column
  // boundary. `table-fixed` + `<colgroup>` constrain the column *layout* but allow unbroken
  // values (long emails, URLs) to *visually* paint over the next column.
  return (
    <TableRoot className={cn("table-fixed", className)}>
      <colgroup>
        {config.columns.map((column) => {
          if (column.fixedWidth !== undefined) {
            return <col key={column.id} style={{ width: column.fixedWidth }} />;
          }
          const relative = column.relativeWidth ?? 1;
          const widthPercent = totalRelative > 0 ? (relative / totalRelative) * 100 : undefined;
          return <col key={column.id} style={{ width: widthPercent !== undefined ? `${widthPercent}%` : undefined }} />;
        })}
      </colgroup>
      {hasHeaders ? (
        <TableHeader>
          <TableRow className="hover:bg-transparent data-[state=selected]:bg-transparent">
            {config.columns.map((column) => (
              <TableHead key={column.id}>
                <div className={cn("flex items-center gap-2", column.headerClassName)}>
                  <div className="min-w-0 overflow-hidden">
                    {flexRender(column.header, {
                      column: { id: column.id, sortBy: column.sortBy },
                      sorting: sortingParams,
                    })}
                  </div>
                  {onSortingParamsChange && isDefined(column.sortBy) ? (
                    <SortingToggle
                      sortBy={column.sortBy}
                      sortingParams={sortingParams}
                      onSortingParamsChange={onSortingParamsChange}
                      iconVariant={column.sortingIconVariant}
                    />
                  ) : null}
                </div>
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
      ) : null}
      <TableBody>
        {data.map((item, index) => (
          <TableRow key={resolveRowKey(item, index)} data-row-index={index} className={config.rowClassName?.(item, index)}>
            {config.columns.map((column) => (
              <TableCell key={column.id} className={cn({ "p-0": config.rowLink })}>
                {config.rowLink ? (
                  <RowLink item={item} linkTo={config.rowLink} className={column.cellClassName}>
                    {column.cell(item)}
                  </RowLink>
                ) : (
                  <div className={cn("min-w-0 overflow-hidden", column.cellClassName)}>{column.cell(item)}</div>
                )}
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </TableRoot>
  );
}

/**
 * Prop Types for the RowLink component
 */
type RowLinkProps<ItemType> = React.PropsWithChildren<{
  item: ItemType;
  linkTo?: (item: ItemType) => string;
  className?: string;
}>;

/**
 * Renders a table row as a link if the `linkTo` function is provided.
 */
function RowLink<ItemType>(props: RowLinkProps<ItemType>): React.ReactElement {
  if (props.linkTo === undefined) {
    return <>{props.children}</>;
  }

  const linkTo = props.linkTo(props.item);

  // Inner wrapper carries the column-boundary clip (matches the non-link branch). `className`
  // also stays on `<Link>` so caller overrides like `py-0` still reach the row padding.
  return (
    <Link className={cn("flex min-h-14 w-full items-center px-4 py-1", props.className)} to={linkTo}>
      <div className={cn("min-w-0 flex-1 overflow-hidden", props.className)}>{props.children}</div>
    </Link>
  );
}
