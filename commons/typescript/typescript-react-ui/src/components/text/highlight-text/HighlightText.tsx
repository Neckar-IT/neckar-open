import { isNotDefined } from "@neckar-hub/typescript-utils/lang";
import { cn } from "cn";
import Highlighter from "react-highlight-words";
import { OrNoValue } from "../no-value";
import { useHighlightSource } from "./HighlightContext";

interface HighlightTextProps {
  /**
   * The text that is shown - and can contain highlighted text
   */
  text: string | null | undefined;
  /**
   * The search terms that are highlighted (multiple terms with array). When omitted, falls
   * back to the nearest enclosing [HighlightSource]; passing a value here overrides the
   * source for this single occurrence.
   */
  search?: string | string[] | undefined;
  /**
   * Additional class name for the wrapper span element
   */
  className?: string | undefined;
  /**
   * How the highlighted text is styled - yellow background by default
   */
  highlightClassName?: string | undefined;
  /**
   * The fallback that is rendered if no children are provided
   */
  fallback?: React.ReactNode;

  /**
   * Minimum token length that is highlighted. Tokens shorter than this are dropped before being
   * handed to `react-highlight-words`.
   * Server-side search is unaffected — this filter only decreases visual clutter for the user.
   */
  highlightTokenLimit?: number;

  /**
   * The text is prose with line and paragraph breaks. `DoNotTruncateTextContent` shows every break, `TruncateTextContent` keeps the
   * text on one line with an ellipsis.
   */
  multiline?: boolean;
}

/**
 * Component to highlight search strings in a larger amount of text.
 *
 * Always sets a native `title` attribute with the full text for tooltip on hover.
 * Truncation is handled by the container via CSS, not by this component.
 */
export function HighlightText({
  text,
  search,
  className,
  highlightClassName,
  fallback,
  highlightTokenLimit = 2,
  multiline = false,
}: HighlightTextProps): React.ReactElement {
  const sourceTokens = useHighlightSource();

  if (isNotDefined(text)) {
    return <OrNoValue className={className} fallback={fallback} />;
  }

  const effective = search ?? sourceTokens;
  const tokens = typeof effective === "string" ? [effective] : (effective ?? []);
  const searchWords = tokens.filter((token) => token.length >= highlightTokenLimit);

  return (
    <Highlighter
      autoEscape={true}
      className={cn(multiline && "whitespace-pre-wrap", className)}
      data-multiline-text={multiline ? "" : undefined}
      highlightClassName={highlightClassName}
      searchWords={searchWords}
      textToHighlight={text}
      title={text}
    />
  );
}
