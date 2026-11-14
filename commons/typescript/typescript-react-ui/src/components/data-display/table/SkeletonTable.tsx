import { flexRender } from "@neckar-hub/typescript-react-utils/render";
import { isDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import { Skeleton } from "../../primitives/skeleton/Skeleton";
import { Table as TableRoot, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../primitives/table";
import type { TableConfig } from "./DataTable";

export interface SkeletonTableProps<ItemType, SortCriteria = never> {
  config: TableConfig<ItemType, SortCriteria>;
  /**
   * Number of placeholder rows to render. Should match the page size of the surrounding pagination
   * (typically `dataSliceController.params.pageSize`).
   */
  rowCount: number;
  className?: string;
}

/**
 * Skeleton placeholder for [DataTable] — same column structure (so column widths line up), animated
 * placeholder blocks instead of cell content. Used by [DataSliceView] in pending mode to keep the
 * layout stable while the data loads.
 *
 * Row height defaults to [TableCell]'s `h-14` (56px). Set `config.estimatedRowHeight` only when the
 * real rows are taller (e.g. multi-line cells) so the skeleton matches.
 */
export function SkeletonTable<ItemType, SortCriteria>({
  config,
  rowCount,
  className,
}: SkeletonTableProps<ItemType, SortCriteria>): React.ReactElement {
  const hasHeaders = config.columns.some((column) => column.header !== undefined);
  const totalRelative = config.columns.reduce((sum, col) => (col.fixedWidth === undefined ? sum + (col.relativeWidth ?? 1) : sum), 0);
  const rowStyle = isDefined(config.estimatedRowHeight) ? { height: `${config.estimatedRowHeight}px` } : undefined;

  return (
    <div className={cn("overflow-x-auto", className)}>
      <TableRoot className="table-fixed">
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
                    <div>
                      {flexRender(column.header, {
                        column: { id: column.id, sortBy: column.sortBy },
                        sorting: undefined,
                      })}
                    </div>
                  </div>
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
        ) : null}
        <TableBody>
          {Array.from({ length: rowCount }, (_, rowIndex) => (
            <TableRow key={rowIndex} className="hover:bg-transparent">
              {config.columns.map((column) => (
                <TableCell key={column.id} style={rowStyle}>
                  <Skeleton className="h-4 w-3/4" />
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </TableRoot>
    </div>
  );
}
