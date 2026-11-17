/**
 * Imports
 */

import { cn } from "cn";
import { DataContainerContent, type DataContainerContentProps } from "./DataContainerContent";
import { DataContainerFooter, type DataContainerFooterProps } from "./DataContainerFooter";
import { DataContainerHeader, type DataContainerHeaderProps } from "./DataContainerHeader";

/**
 * Prop Types
 */

export type DataContainerProps = DataContainerHeaderProps &
  DataContainerContentProps &
  DataContainerFooterProps & {
    className?: string | undefined;
  };

/**
 * Deprecated layout wrapper for paginated tables with header, content, and footer slots.
 *
 * @deprecated Use `DataSliceView` with `ResponsiveTable` instead.
 */

export function DataContainer({
  children,
  topLeftComponents,
  topRightComponents,
  isFetching,
  className,
  paginationInfo,
  onSetPagination,
  disabledPagination,
  pageSizeOptions,
  textKeys,
}: DataContainerProps) {
  return (
    <div className={cn("space-y-2", className)}>
      {/* oxlint-disable-next-line typescript/no-deprecated -- the deprecated DataContainer is composed of its own deprecated sub-components */}
      <DataContainerHeader
        isFetching={isFetching}
        topLeftComponents={topLeftComponents}
        topRightComponents={topRightComponents}
        textKeys={textKeys}
      />

      <div className="rounded-md border">
        {/* oxlint-disable-next-line typescript/no-deprecated -- the deprecated DataContainer is composed of its own deprecated sub-components */}
        <DataContainerContent textKeys={textKeys} paginationInfo={paginationInfo}>
          {children}
        </DataContainerContent>
      </div>

      {/* oxlint-disable-next-line typescript/no-deprecated -- the deprecated DataContainer is composed of its own deprecated sub-components */}
      <DataContainerFooter
        disabledPagination={disabledPagination}
        pageSizeOptions={pageSizeOptions}
        paginationInfo={paginationInfo}
        onSetPagination={onSetPagination}
        textKeys={textKeys}
      />
    </div>
  );
}
