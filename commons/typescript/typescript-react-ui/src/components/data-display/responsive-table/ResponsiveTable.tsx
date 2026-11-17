import { flexRender } from "@neckar-hub/typescript-react-utils/render";
import { cn } from "cn";
import { Fragment, useMemo, type ReactElement } from "react";
import { Link } from "react-router";
import { type TableConfig, type TableProps, DataTable, type ColumnDefinition, resolveRowKey } from "../table";

export type ResponsiveColumnDefinition<ItemType, SortCriteria = never> = ColumnDefinition<ItemType, SortCriteria> & {
  /**
   * This allows the user to specify how a column should be displayed in mobile devices
   * - `default`: key-value pair in the details section of the card — a `cell` returning `null` for
   *   an item drops the pair, so a column that has nothing for this row shows no label either
   * - `primary-header`: displayed prominently on the left in the header section of the card (only one column should have this)
   * - `secondary-header`: displayed on the right in the header section but with less emphasis (smaller font, different color) (only one column should have this)
   * - `none`: hidden on mobile devices
   */
  viewOnMobile?: "default" | "primary-header" | "secondary-header" | "none";
};

export type ResponsiveTableConfig<ItemType, SortCriteria = never> = Omit<TableConfig<ItemType, SortCriteria>, "columns"> & {
  columns: ResponsiveColumnDefinition<ItemType, SortCriteria>[];
};

export type ResponsiveTableProps<ItemType, SortCriteria = never> = Omit<TableProps<ItemType, SortCriteria>, "config"> & {
  config: ResponsiveTableConfig<ItemType, SortCriteria>;
};

/**
 * Data table that renders a list of items by column config, collapsing to stacked cards on mobile.
 *
 * Pure data renderer: the pending/loading state is handled by the surrounding
 * `DataSliceView`, not here.
 */

export function ResponsiveTable<ItemType, SortCriteria>({
  className,
  config,
  ...rest
}: ResponsiveTableProps<ItemType, SortCriteria>): ReactElement {
  // The desktop show/hide toggle lives on a wrapper `<div>`, not on the `<table>` itself.
  // Putting `@3xl:block` on the `<table>` would set `display: block`, overriding its
  // `display: table` and disabling `table-fixed` + the `<colgroup>` widths, so the table
  // would shrink to content instead of filling the container.
  return (
    <div className={cn("@container", className)}>
      <CardView data={rest.data} config={config} className="@3xl:hidden" />
      <div className="hidden @3xl:block">
        <DataTable {...rest} config={config} />
      </div>
    </div>
  );
}

/**
 * Card View for mobile screens
 */
function CardView<ItemType, SortCriteria>({
  data,
  config,
  className,
}: {
  data: ItemType[];
  config: ResponsiveTableConfig<ItemType, SortCriteria>;
  className?: string;
}): ReactElement {
  const primaryHeaderIndex = useMemo(() => config.columns.findIndex((col) => col.viewOnMobile === "primary-header"), [config.columns]);
  const secondaryHeaderIndex = useMemo(() => config.columns.findIndex((col) => col.viewOnMobile === "secondary-header"), [config.columns]);
  const primaryHeaderColumn = primaryHeaderIndex >= 0 ? config.columns[primaryHeaderIndex] : undefined;
  const secondaryHeaderColumn = secondaryHeaderIndex >= 0 ? config.columns[secondaryHeaderIndex] : undefined;
  const detailColumns = useMemo(
    () =>
      config.columns.filter((col, index) => col.viewOnMobile !== "none" && index !== primaryHeaderIndex && index !== secondaryHeaderIndex),
    [config.columns, primaryHeaderIndex, secondaryHeaderIndex],
  );

  return (
    <div className={className}>
      <div className="divide-border divide-y">
        {data.map((item, index) => {
          const itemContent = (
            <div className="p-4">
              {primaryHeaderColumn || secondaryHeaderColumn ? (
                <div className="flex items-center justify-between pb-1">
                  {/* `min-w-0` lets the flex children shrink so nested `data-text-content` spans can clip with ellipsis instead of pushing the card wider (#1924). */}
                  {primaryHeaderColumn ? <div className="min-w-0 font-semibold">{primaryHeaderColumn.cell(item)}</div> : null}
                  {secondaryHeaderColumn ? (
                    <div className="text-muted-foreground font-semibold">{secondaryHeaderColumn.cell(item)}</div>
                  ) : null}
                </div>
              ) : null}
              {detailColumns.length > 0 ? (
                <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
                  {detailColumns.map((col) => {
                    const content = col.cell(item);
                    if (content === null || content === undefined) {
                      return null;
                    }
                    return (
                      <Fragment key={col.id}>
                        <dt className="text-muted-foreground text-sm font-light">
                          {/* The header does not allow sorting, so we can hardcode the context here */}
                          {flexRender(col.header, { column: { id: col.id, sortBy: col.sortBy }, sorting: undefined })}
                        </dt>
                        {/* `min-w-0` lets the `1fr` grid track shrink so nested `data-text-content` clips with ellipsis instead of overflowing the card. */}
                        <dd className="min-w-0 text-sm font-medium">{content}</dd>
                      </Fragment>
                    );
                  })}
                </dl>
              ) : null}
            </div>
          );

          const rowClassName = config.rowClassName?.(item, index);

          if (config.rowLink) {
            return (
              <Link
                key={resolveRowKey(item, index)}
                to={config.rowLink(item)}
                className={cn("hover:bg-accent block transition-colors", rowClassName)}
              >
                {itemContent}
              </Link>
            );
          }

          return (
            <div key={resolveRowKey(item, index)} className={rowClassName}>
              {itemContent}
            </div>
          );
        })}
      </div>
    </div>
  );
}
