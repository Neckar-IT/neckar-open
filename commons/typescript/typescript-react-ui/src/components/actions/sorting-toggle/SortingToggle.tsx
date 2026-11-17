/**
 * Imports
 */

import { Button } from "@neckar-hub/typescript-react-ui/actions";
import type { OnSortingParamsChange, SortingParams } from "@neckar-hub/typescript-react-utils/search-params";
import { SortOrder } from "@neckar-hub/typescript-utils/openapi-types";
import { ArrowDown01Icon, ArrowDown10Icon, ArrowDownAZIcon, ArrowDownZAIcon, ArrowUpDownIcon } from "lucide-react";

/**
 * Prop Types
 */

export type SortingIconVariant = "alphabetical" | "numerical";

interface SortingToggleProps<T> {
  sortBy: T;
  sortingParams: SortingParams<T> | undefined;
  onSortingParamsChange: OnSortingParamsChange<T>;
  iconVariant?: SortingIconVariant;
}

/**
 * Component to toggle through sorting options
 *
 * @prop {T} sortBy - The value of sort by, when the sorting for this column is active
 * @prop {SortingParams<T> | undefined} sortingValue - The current sorting value, or undefined if no sorting is active
 * @prop {OnSortingParamsChange<T>} onSortingValueChange - Callback to change the sorting value
 * @prop {"alphabetical" | "numerical"} iconVariant - Used to selected the icon to display the sorting order
 */

export function SortingToggle<T>(props: SortingToggleProps<T>): React.ReactElement {
  const iconVariant = props.iconVariant ?? "alphabetical";

  const handleClick = () => {
    // We cycle through the sort orders: undefined (no sorting) -> Asc -> Desc
    switch (props.sortingParams?.sortOrder) {
      case undefined:
        props.onSortingParamsChange({ sortBy: props.sortBy, sortOrder: SortOrder.Asc });
        break;

      case SortOrder.Asc:
        props.onSortingParamsChange({ sortBy: props.sortBy, sortOrder: SortOrder.Desc });
        break;

      case SortOrder.Desc:
        props.onSortingParamsChange(undefined);
        break;
      default:
        throw new Error(`Unexpected value in Switch Statement: props.sortingParams?.sortOrder`);
    }
  };

  // Sort is active for this column => show the sorting icon based on the current sort order
  if (props.sortingParams?.sortBy === props.sortBy) {
    return (
      <Button className="h-8 w-8" size="icon" variant="ghost" onClick={handleClick}>
        <SortToggleIcon sortOrder={props.sortingParams.sortOrder} iconVariant={iconVariant} />
      </Button>
    );
  }

  // Sort is not active for this column => show a default icon
  return (
    <Button className="h-8 w-8" size="icon" variant="ghost" onClick={handleClick}>
      <ArrowUpDownIcon />
    </Button>
  );
}

/**
 * Icon
 */

function SortToggleIcon({
  sortOrder,
  iconVariant: variant,
}: {
  sortOrder: SortOrder | undefined;
  iconVariant: SortingIconVariant;
}): React.ReactElement {
  switch (variant) {
    case "alphabetical":
      return sortOrder === SortOrder.Asc || sortOrder === undefined ? <ArrowDownAZIcon /> : <ArrowDownZAIcon />;

    case "numerical":
      return sortOrder === SortOrder.Asc || sortOrder === undefined ? <ArrowDown01Icon /> : <ArrowDown10Icon />;
    default:
      throw new Error(`Unexpected value in Switch Statement: variant`);
  }
}
