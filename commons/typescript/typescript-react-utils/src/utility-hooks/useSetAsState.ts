/**
 * Imports
 */

import { useMemo, useState } from "react";

/**
 * Return Type
 */

export interface SetAsState<T> {
  has: (value: T) => boolean;
  add: (value: T) => void;
  delete: (value: T) => void;
  clear: () => void;
}

/**
 * Custom Hook to handle a set with Rreact's useState.
 */

export function useSetAsState<T>(initialValues: T[] = []): SetAsState<T> {
  const [state, setState] = useState(() => new Set(initialValues));

  return useMemo(() => {
    return {
      has: (value: T) => state.has(value),
      add: (value: T) => {
        setState((prev) => new Set([...prev, value]));
      },
      delete: (value: T) => {
        setState((prev) => {
          const newSet = new Set(prev);
          newSet.delete(value);
          return newSet;
        });
      },
      clear: () => {
        setState(new Set());
      },
    };
  }, [state]);
}
