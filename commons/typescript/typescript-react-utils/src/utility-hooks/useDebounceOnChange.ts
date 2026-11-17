/**
 * Imports
 */

import { useState } from "react";
import { useDebounceCallback } from "usehooks-ts";

/**
 * Param Types
 */

interface UseDebounceOnChangeParams<T> {
  initialValue: T | undefined;
  debouncedCallback: (value: T | undefined) => void;
  delay?: number | undefined;
}

/**
 * Return Type
 */

interface UseDebounceOnChangeReturnType<T> {
  inputValue: T | undefined;
  handleChange: (value: T | undefined) => void;
}

/**
 * Hook to debounce the onChange event of an input
 *
 * @param
 */

export function useDebounceOnChange<T>(params: UseDebounceOnChangeParams<T>): UseDebounceOnChangeReturnType<T> {
  const [inputValue, setInputValue] = useState<T | undefined>(params.initialValue);
  const debouncedOnChange = useDebounceCallback(params.debouncedCallback, params.delay ?? 300);

  const handleChange = (value: T | undefined) => {
    setInputValue(value);
    debouncedOnChange(value);
  };

  return {
    inputValue,
    handleChange,
  };
}
