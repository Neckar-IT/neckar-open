import type { ReactElement, ReactNode } from "react";

export interface DataSliceToolbarProps {
  /** The filters of the slice, typically `<Filters>`. */
  filters?: ReactNode;
  /** The actions on the slice, e.g. the button that creates an entity; at the end of the row. */
  actions?: ReactNode;
}

/**
 * The toolbar of a `DataSliceView`: the filters at the start, the actions at the end. Below the container width
 * `@3xl` the actions wrap below the filters.
 */
export function DataSliceToolbar({ filters, actions }: DataSliceToolbarProps): ReactElement {
  return (
    <div className="flex flex-col items-start justify-between gap-2 @3xl:flex-row">
      <div className="flex w-full min-w-0 flex-wrap items-center gap-x-4 gap-y-2 @3xl:flex-1">{filters}</div>
      {actions}
    </div>
  );
}
