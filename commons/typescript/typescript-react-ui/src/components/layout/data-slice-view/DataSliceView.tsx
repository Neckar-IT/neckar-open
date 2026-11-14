/**
 * Imports
 */

import type { OnPaginationParamsChange } from "@neckar-hub/typescript-react-utils/search-params";
import type { PaginationInfo } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import type { ReactElement, ReactNode } from "react";
import { useEmbeddedTranslation } from "../../../localization/useEmbeddedTranslation";
import { ErrorIndicator } from "../../feedback/error-indicator";
import { LoadingIndicator } from "../../feedback/loading-indicator";
import { NotFoundIndicator } from "../../feedback/not-found-indicator";
import { Pagination } from "../../navigation/pagination";
import { Skeleton } from "../../primitives/skeleton/Skeleton";
import { TableFrame } from "../table-frame";

/**
 * Prop Types
 *
 * `DataSliceView` is the layout wrapper for a paginated/filtered/sorted data
 * slice (table, grid, card list, ...). It coordinates toolbar (filters + actions),
 * content (data or pending indicator) and footer (pagination).
 *
 * Two modes, encoded as a discriminated union on `pending`:
 *
 * - **Pending mode** (`pending: true`): no data yet (first load, or parameter
 *   change that invalidates the current data). The toolbar stays visible so the
 *   user can still interact with the filters. Content renders the optional
 *   `children` (typically `<SkeletonResponsiveTable>`); falls back to a plain
 *   `<LoadingIndicator />` when `children` are omitted. Footer renders skeleton
 *   placeholders that mirror the resolved-mode footer layout, so the page does
 *   not shift vertically when data resolves.
 *
 * - **Resolved mode** (`pending` omitted or `false`): data is present. Toolbar,
 *   content (children), and footer (pagination) are rendered. `children` holds
 *   the concrete renderer, typically `<ResponsiveTable>`.
 */

type DataSliceViewCommonProps = {
  className?: string;
  hideBorder?: boolean;
  /** Above the content, typically a `DataSliceToolbar`. */
  toolbar?: ReactNode;
};

type DataSliceViewPendingProps = DataSliceViewCommonProps & {
  pending: true;
  /**
   * Optional pending placeholder, typically `<SkeletonResponsiveTable>`. Falls back to a plain
   * `<LoadingIndicator />` when omitted.
   */
  children?: ReactNode;
};

type DataSliceViewResolvedProps = DataSliceViewCommonProps & {
  pending?: false;
  paginationInfo: PaginationInfo;
  onPaginationParamsChange: OnPaginationParamsChange;
  pageSizeOptions?: number[];
  paginationDisabled?: boolean;
  /** Shown while the slice holds no record at all. Without it, an empty slice shows the generic `dataSliceView.noData`. */
  empty?: ReactNode;
  children?: ReactNode;
};

export type DataSliceViewProps = DataSliceViewPendingProps | DataSliceViewResolvedProps;

/**
 * Layout wrapper for a paginated, filtered, and sorted data slice with toolbar, content, and pagination footer.
 *
 * The root is the `@container` the toolbar switches its layout on; the table measures itself.
 */

export function DataSliceView(props: DataSliceViewProps): ReactElement {
  return (
    <div className={cn("@container flex flex-col space-y-2", props.className)}>
      {props.toolbar}

      <OptionalTableFrame hideBorder={props.hideBorder}>
        {props.pending ? (
          (props.children ?? <LoadingIndicator className="h-80" />)
        ) : (
          <DataSliceViewContent paginationInfo={props.paginationInfo} empty={props.empty}>
            {props.children}
          </DataSliceViewContent>
        )}
      </OptionalTableFrame>

      {props.pending ? (
        <DataSliceViewSkeletonFooter />
      ) : (
        <DataSliceViewFooter
          paginationInfo={props.paginationInfo}
          paginationDisabled={props.paginationDisabled}
          pageSizeOptions={props.pageSizeOptions}
          onPaginationParamsChange={props.onPaginationParamsChange}
        />
      )}
    </div>
  );
}

/** A caller that already sits inside a frame passes `hideBorder` so the two do not nest. */
function OptionalTableFrame({ hideBorder, children }: { hideBorder?: boolean; children: ReactNode }): ReactElement {
  if (hideBorder === true) {
    return <>{children}</>;
  }

  return <TableFrame>{children}</TableFrame>;
}

/**
 * Prop Types for the DataSliceView Content
 */

interface DataSliceViewContentProps {
  paginationInfo: PaginationInfo;
  empty?: ReactNode;
  children?: ReactNode;
}

/**
 * Content Component for the DataSliceView
 */

function DataSliceViewContent(props: DataSliceViewContentProps): ReactNode {
  // No Items in the database
  if (props.paginationInfo.totalElementsCount === 0) {
    if (props.empty === undefined) {
      return <NotFoundIndicator textKey="dataSliceView.noData" className="h-80" />;
    }
    return props.empty;
  }

  // No items match the filters
  if (props.paginationInfo.filteredElementsCount === 0) {
    return <NotFoundIndicator textKey="dataSliceView.nothingFound" className="h-80" />;
  }

  // invalid pagination
  if (props.paginationInfo.pageIndex >= props.paginationInfo.pageCount) {
    return <ErrorIndicator textKey="dataSliceView.invalidPagination" className="h-80" />;
  }

  return props.children;
}

/**
 * Prop Types for the DataSliceView Footer
 */

interface DataSliceViewFooterProps {
  paginationInfo: PaginationInfo;
  onPaginationParamsChange: OnPaginationParamsChange;
  pageSizeOptions?: number[];
  paginationDisabled?: boolean;
}

/**
 * Footer Component for the DataSliceView
 */

function DataSliceViewFooter(props: DataSliceViewFooterProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const itemsText = t({
    key: "dataSliceView.items",
    options: {
      filteredElementsCount: props.paginationInfo.filteredElementsCount,
      totalElementsCount: props.paginationInfo.totalElementsCount,
    },
  });

  // TODO: change how the pagination component uses the pagination params
  const handlePaginationChange = (pageIndex: number, pageSize: number) => {
    props.onPaginationParamsChange({ pageIndex, pageSize });
  };

  return (
    <div className="flex flex-col items-end justify-between space-y-2 lg:flex-row lg:items-center lg:space-y-0">
      <p className="text-muted-foreground text-sm">{itemsText}</p>
      <Pagination
        disabled={props.paginationDisabled}
        pageCount={props.paginationInfo.pageCount}
        pageIndex={props.paginationInfo.pageIndex}
        pageSize={props.paginationInfo.pageSize}
        pageSizeOptions={props.pageSizeOptions}
        onChange={handlePaginationChange}
      />
    </div>
  );
}

/**
 * Skeleton placeholder for [DataSliceViewFooter] — mirrors the resolved footer's outer flex layout
 * (items text on the left, pagination on the right) and the [Pagination] component's internal
 * structure (page-size selector + page indicator + 4 navigation buttons), so the footer occupies the
 * same vertical space in pending mode and prevents layout shift when data resolves.
 */
function DataSliceViewSkeletonFooter(): React.ReactElement {
  return (
    <div className="flex flex-col items-end justify-between space-y-2 lg:flex-row lg:items-center lg:space-y-0">
      <Skeleton className="h-4 w-48" />
      <div className="flex flex-col items-end space-y-2 sm:flex-row sm:items-center sm:space-y-0 sm:space-x-6 lg:space-x-8">
        <div className="flex items-center space-x-2">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-8 w-[70px]" />
        </div>
        <div className="flex space-x-2">
          <div className="flex items-center">
            <Skeleton className="h-4 w-20" />
          </div>
          <div className="flex items-center">
            <Skeleton className="h-8 w-8 rounded-se-none rounded-ee-none" />
            <Skeleton className="h-8 w-8 rounded-none" />
            <Skeleton className="h-8 w-8 rounded-none" />
            <Skeleton className="h-8 w-8 rounded-ss-none rounded-es-none" />
          </div>
        </div>
      </div>
    </div>
  );
}
