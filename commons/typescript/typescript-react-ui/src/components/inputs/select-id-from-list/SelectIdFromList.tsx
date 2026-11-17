/**
 * Imports
 */

import { Select, SelectItem, type SelectProps } from "../select";

/**
 * Prop Types
 */

interface SelectIdFromListProps<T extends { id: string }> extends Omit<SelectProps, "children"> {
  options: T[];
  itemRenderer: (item: T) => React.ReactNode;
}

/**
 * Select dropdown that binds to the id field of items in a typed list; value and onChange operate on the string id.
 */

export function SelectIdFromList<T extends { id: string }>({
  options,
  itemRenderer,
  ...rest
}: SelectIdFromListProps<T>): React.ReactElement {
  return (
    <Select {...rest}>
      {options.map((option) => (
        <SelectItem key={option.id} value={option.id}>
          {itemRenderer(option)}
        </SelectItem>
      ))}
    </Select>
  );
}
