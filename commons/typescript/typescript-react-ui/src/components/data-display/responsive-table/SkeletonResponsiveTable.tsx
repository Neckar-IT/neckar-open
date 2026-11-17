import { cn } from "cn";
import { useMemo, type ReactElement } from "react";
import { Skeleton } from "../../primitives/skeleton/Skeleton";
import { SkeletonTable } from "../table";
import type { ResponsiveTableConfig } from "./ResponsiveTable";

export interface SkeletonResponsiveTableProps<ItemType, SortCriteria> {
  config: ResponsiveTableConfig<ItemType, SortCriteria>;
  /**
   * Number of placeholder rows / cards to render. Should match the page size of the surrounding
   * pagination (typically `dataSliceController.params.pageSize`).
   */
  rowCount: number;
  className?: string;
}

/**
 * Skeleton placeholder for [ResponsiveTable] — same column / card structure as the resolved view,
 * animated placeholder blocks instead of cell content. Used by [DataSliceView] in pending mode to
 * keep the layout stable while the data loads.
 *
 * Mirrors [ResponsiveTable]: a card list on mobile, a regular table on desktop.
 */
export function SkeletonResponsiveTable<ItemType, SortCriteria>({
  config,
  rowCount,
  className,
}: SkeletonResponsiveTableProps<ItemType, SortCriteria>): ReactElement {
  return (
    <div className={cn("@container", className)}>
      <SkeletonCardView config={config} rowCount={rowCount} className="@3xl:hidden" />
      <SkeletonTable className="hidden @3xl:block" config={config} rowCount={rowCount} />
    </div>
  );
}

/**
 * Card-View skeleton for mobile screens — mirrors the [CardView] in [ResponsiveTable].
 */
function SkeletonCardView<ItemType, SortCriteria>({
  config,
  rowCount,
  className,
}: {
  config: ResponsiveTableConfig<ItemType, SortCriteria>;
  rowCount: number;
  className?: string;
}): ReactElement {
  const hasPrimaryHeader = useMemo(() => config.columns.some((col) => col.viewOnMobile === "primary-header"), [config.columns]);
  const hasSecondaryHeader = useMemo(() => config.columns.some((col) => col.viewOnMobile === "secondary-header"), [config.columns]);
  const detailColumns = useMemo(
    () =>
      config.columns.filter(
        (col) => col.viewOnMobile !== "none" && col.viewOnMobile !== "primary-header" && col.viewOnMobile !== "secondary-header",
      ),
    [config.columns],
  );

  return (
    <div className={className}>
      <div className="divide-border divide-y">
        {Array.from({ length: rowCount }, (_, rowIndex) => (
          <div key={rowIndex} className="p-4">
            {hasPrimaryHeader || hasSecondaryHeader ? (
              <div className="flex items-center justify-between pb-1">
                {hasPrimaryHeader ? <Skeleton className="h-5 w-1/2" /> : null}
                {hasSecondaryHeader ? <Skeleton className="h-4 w-1/4" /> : null}
              </div>
            ) : null}
            {detailColumns.length > 0 ? (
              <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
                {detailColumns.map((col) => (
                  <div key={col.id} className="contents">
                    <dt>
                      <Skeleton className="h-3 w-20" />
                    </dt>
                    <dd>
                      <Skeleton className="h-4 w-2/3" />
                    </dd>
                  </div>
                ))}
              </dl>
            ) : null}
          </div>
        ))}
      </div>
    </div>
  );
}
