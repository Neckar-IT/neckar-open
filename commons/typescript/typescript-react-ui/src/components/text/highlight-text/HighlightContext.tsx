import { createContext, useContext, type PropsWithChildren, type ReactElement } from "react";

type HighlightTokens = string | string[] | undefined;

const HighlightContext = createContext<HighlightTokens>(undefined);

/**
 * Provides highlight tokens for any [HighlightText] descendant. Counterpart to [HighlightText]:
 * source sets, sink reads. Use to lift the highlight state out of multi-layer prop drilling
 * (e.g. wrap a paginated table so its cells highlight matches without each cell knowing about
 * the search state).
 */
export function HighlightSource({ tokens, children }: PropsWithChildren<{ tokens: HighlightTokens }>): ReactElement {
  return <HighlightContext.Provider value={tokens}>{children}</HighlightContext.Provider>;
}

/**
 * Returns the highlight tokens of the nearest enclosing [HighlightSource], or `undefined`
 * if there is no source above. Consumed by [HighlightText] as fallback when no explicit
 * `search` prop is passed.
 */
export function useHighlightSource(): HighlightTokens {
  return useContext(HighlightContext);
}
