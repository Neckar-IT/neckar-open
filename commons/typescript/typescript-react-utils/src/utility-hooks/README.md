# Utility Hooks

## Re-exports from `usehooks-ts`

`index.ts` re-exports these hooks from [usehooks-ts](https://usehooks-ts.com/), so callers keep importing from `@neckar-hub/typescript-react-utils/utility-hooks`:

`useBoolean`, `useCounter`, `useDebounceCallback`, `useDebounceValue`, `useInterval`, `useIsMounted`, `useIsomorphicLayoutEffect`, `useMediaQuery`, `useUnmount`, type `DebouncedState`.

## Wrapper

`useCountdown` wraps the `usehooks-ts` hook and adds `initiallyRunning`, which upstream lacks. The countdown starts in a layout effect on mount and stays stopped in test mode (`isTestMode()`), so screenshot tests see a fixed count.

## Own hooks

`useCurrentDevice`, `useCustomEvent`, `useDebounceOnChange`, `usePaginationState`, `useSetAsState`.

`usePaginationState` keeps the pagination of a list in the component rather than in the URL. Its URL sibling is `usePaginationSearchParams` in `../search-params/`, and both read `PaginationParams` and the defaults from `../search-params/paginationParams.ts`.
