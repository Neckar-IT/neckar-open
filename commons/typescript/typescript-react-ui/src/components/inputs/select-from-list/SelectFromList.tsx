/**
 * Imports
 */

import { isEqual } from "lodash";
import { useMemo } from "react";
import { Select, SelectItem, type SelectProps } from "../select";

/**
 * Prop Types
 */

export interface SelectFromListProps<T> extends Omit<SelectProps, "children" | "value" | "onChange"> {
  options: T[];
  itemRenderer: (item: T) => React.ReactNode;
  value: T | undefined | null;
  onChange: (value: T | null) => void;
}

/**
 * Select dropdown that binds to a typed object list, using deep equality to match the selected value.
 */

export function SelectFromList<T>({ options, itemRenderer, value, onChange, ...rest }: SelectFromListProps<T>): React.ReactElement {
  const selectedIndex = useMemo(() => {
    if (value === null || value === undefined) {
      return null;
    }

    const index = options.findIndex((item) => isEqual(item, value));

    if (index === -1) {
      return null;
    }

    return index.toString();
  }, [options, value]);

  const handleChange = (newValue: string | null) => {
    if (newValue === null) {
      onChange(null);
      return;
    }

    const index = Number(newValue);
    const selectedOption = options[index];

    if (isNaN(index) || selectedOption === undefined) {
      throw new Error(`Invalid index selected: [${newValue}]`);
    }

    onChange(selectedOption);
  };

  return (
    <Select {...rest} value={selectedIndex} onChange={handleChange}>
      {options.map((option, index) => (
        <SelectItem key={index} value={index.toString()}>
          {itemRenderer(option)}
        </SelectItem>
      ))}
    </Select>
  );
}
