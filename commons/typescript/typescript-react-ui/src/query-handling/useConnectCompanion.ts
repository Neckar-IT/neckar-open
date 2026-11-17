import { type QueryKey, useQueryClient } from "@neckar-hub/typescript-react-query";
import { useDebounceValue } from "@neckar-hub/typescript-react-utils/utility-hooks";
import { useEffect, useRef } from "react";

/**
 * Prop Types
 */
export interface UseConnectCompanionOptions<InputType, DerivedType> {
  /**
   * The calculation's current inputs, as the form holds them
   */
  input: InputType;
  /**
   * Structural equality for inputs - a debounce round only starts when the input actually changed
   */
  inputEquals: (left: InputType, right: InputType) => boolean;
  /**
   * No requests while false (e.g. the form is pristine or disabled)
   */
  enabled: boolean;
  /**
   * Cache key for the companion request
   */
  queryKey: (input: InputType) => QueryKey;
  /**
   * Fetches the derived value from the server. Return undefined to ignore the response (e.g. a bad
   * response envelope) - null is a valid derived value.
   */
  fetchValue: (input: InputType) => Promise<DerivedType | undefined>;
  /**
   * Applies the derived value to what the client shows
   */
  applyValue: (derived: DerivedType) => void;
  /**
   * Debounce for input changes. Defaults to 300 ms.
   */
  debounceMs?: number;
  /**
   * How long a companion value for identical inputs is served from the cache. Defaults to 30 s.
   */
  staleTimeMs?: number;
}

/**
 * Keeps a server-calculated derived value in sync with its inputs while the user edits
 * (form-companion pattern).
 *
 * Every debounced input change fetches the companion value and applies it to the display. The companion
 * is never authoritative - on save the server recalculates from the submitted fields, so a stale or
 * missed companion value cannot produce wrong data. The calculation itself stays on the server; this hook only moves
 * its result.
 */
export function useConnectCompanion<InputType, DerivedType>({
  input,
  inputEquals,
  enabled,
  queryKey,
  fetchValue,
  applyValue,
  debounceMs = 300,
  staleTimeMs = 30_000,
}: UseConnectCompanionOptions<InputType, DerivedType>): void {
  const queryClient = useQueryClient();

  const [debouncedInput] = useDebounceValue<InputType>(input, debounceMs, { trailing: true, equalityFn: inputEquals });

  // The callbacks go through refs: callers pass inline lambdas, and a round trip per render would
  // defeat the debounce. The effect reacts to input changes, not to new callback identities.
  const queryKeyRef = useRef(queryKey);
  queryKeyRef.current = queryKey;
  const fetchValueRef = useRef(fetchValue);
  fetchValueRef.current = fetchValue;
  const applyValueRef = useRef(applyValue);
  applyValueRef.current = applyValue;

  useEffect(() => {
    if (!enabled) return;

    let cancelled = false;
    void (async () => {
      // The result is wrapped: react-query rejects a queryFn that resolves to undefined, but undefined
      // is this hook's ignore-sentinel (null is a valid derived value).
      let result: { derived: DerivedType | undefined };
      try {
        result = await queryClient.query({
          queryKey: queryKeyRef.current(debouncedInput),
          queryFn: async () => ({ derived: await fetchValueRef.current(debouncedInput) }),
          staleTime: staleTimeMs,
        });
      } catch {
        // A failed companion request changes nothing the user sees - the server recalculates on save anyway.
        return;
      }
      if (cancelled || result.derived === undefined) return;

      applyValueRef.current(result.derived);
    })();

    return () => {
      cancelled = true;
    };
  }, [debouncedInput, enabled, queryClient, staleTimeMs]);
}
